/**
 * Copyright 2010-2012 The PlayN Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package playn.core.gl;

import playn.core.Asserts;
import playn.core.Image;
import playn.core.InternalTransform;
import playn.core.Pattern;

public abstract class ImageGL implements Image {

  /** The current count of references to this image. */
  protected int refs;

  /** Our texture and repeatable texture handles. */
  protected Object tex, reptex;

  /**
   * Creates a texture for this image (if one does not already exist) and returns it. May return
   * null if the underlying image data is not yet ready.
   */
  public Object ensureTexture(GLContext ctx, boolean repeatX, boolean repeatY) {
    if (!isReady()) {
      return null;
    } else if (repeatX || repeatY) {
      scaleTexture(ctx, repeatX, repeatY);
      return reptex;
    } else {
      loadTexture(ctx);
      return tex;
    }
  }

  /**
   * Releases this image's texture memory.
   */
  public void clearTexture(GLContext ctx) {
    if (tex != null) {
      ctx.destroyTexture(tex);
      tex = null;
    }
    if (reptex != null) {
      ctx.destroyTexture(reptex);
      reptex = null;
    }
  }

  /**
   * Increments this image's reference count. Called by {@link ImageLayerGL} to let the image know
   * that it's part of the scene graph. Note that this reference counting mechanism only exists to
   * make more efficient use of texture memory. Images are also used by things like {@link Pattern}
   * which does not support reference counting, thus images must also provide some fallback
   * mechanism for releasing their texture when no longer needed (like in their finalizer).
   */
  public void reference(GLContext ctx) {
    refs++; // we still create our texture on demand
  }

  /**
   * Decrements this image's reference count. Called by {@link ImageLayerGL} to let the image know
   * that may no longer be part of the scene graph.
   */
  public void release(GLContext ctx) {
    Asserts.checkState(refs > 0, "Released an image with no references!");
    if (--refs == 0) {
      clearTexture(ctx);
    }
  }

  /**
   * Draws this image with the supplied transform in the specified target dimensions.
   */
  void draw(GLContext ctx, InternalTransform xform, float dx, float dy, float dw, float dh,
            boolean repeatX, boolean repeatY, float alpha) {
    Object tex = ensureTexture(ctx, repeatX, repeatY);
    if (tex != null) {
      float sw = repeatX ? dw : width(), sh = repeatY ? dh : height();
      ctx.drawTexture(tex, texWidth(repeatX), texHeight(repeatY), xform,
                      dx, dy, dw, dh, x(), y(), sw, sh, alpha);
    }
  }

  /**
   * Draws this image with the supplied transform, and source and target dimensions.
   */
  void draw(GLContext ctx, InternalTransform xform, float dx, float dy, float dw, float dh,
            float sx, float sy, float sw, float sh, float alpha) {
    Object tex = ensureTexture(ctx, false, false);
    if (tex != null) {
      ctx.drawTexture(tex, texWidth(false), texHeight(false), xform,
                      dx, dy, dw, dh, x()+sx, y()+sy, sw, sh, alpha);
    }
  }

  /**
   * The x offset into our source image at which this image's region starts.
   */
  protected float x() {
    return 0;
  }

  /**
   * The y offset into our source image at which this image's region starts.
   */
  protected float y() {
    return 0;
  }

  /**
   * Returns the width of our underlying texture image.
   */
  protected float texWidth(boolean repeatX) {
    return width();
  }

  /**
   * Returns the height of our underlying texture image.
   */
  protected float texHeight(boolean repeatY) {
    return height();
  }

  /**
   * Copies our current image data into the supplied texture.
   */
  protected abstract void updateTexture(GLContext ctx, Object tex);

  private void loadTexture(GLContext ctx) {
    if (tex != null)
      return;
    tex = ctx.createTexture(false, false);
    updateTexture(ctx, tex);
  }

  private void scaleTexture(GLContext ctx, boolean repeatX, boolean repeatY) {
    if (reptex != null)
      return;

    int scaledWidth = ctx.scaledCeil(width());
    int scaledHeight = ctx.scaledCeil(height());

    // GL requires pow2 on axes that repeat
    int width = GLUtil.nextPowerOfTwo(scaledWidth), height = GLUtil.nextPowerOfTwo(scaledHeight);

    // TODO: if width/height > platform_max_size, repeatedly scale by 0.5 until within bounds
    // platform_max_size = 1024 for iOS, GL10.GL_MAX_TEXTURE_SIZE on android, etc.

    // no need to scale if our source data is already a power of two
    if ((width == 0) && (height == 0)) {
      reptex = ctx.createTexture(scaledWidth, scaledHeight, repeatX, repeatY);
      updateTexture(ctx, reptex);
      return;
    }

    // otherwise we need to scale our non-repeated texture, so load that normally
    loadTexture(ctx);

    // width/height == 0 => already a power of two.
    if (width == 0)
      width = scaledWidth;
    if (height == 0)
      height = scaledHeight;

    // create our texture and point a new framebuffer at it
    reptex = ctx.createTexture(width, height, repeatX, repeatY);
    Object fbuf = ctx.createFramebuffer(reptex);

    // render the non-repeated texture into the framebuffer properly scaled
    ctx.bindFramebuffer(fbuf, width, height);
    ctx.clear(0, 0, 0, 0);
    ctx.drawTexture(tex, width(), height(), ctx.createTransform(),
                    0, height, width, -height, false, false, 1);

    // we no longer need this framebuffer; rebind the default framebuffer and delete ours
    ctx.bindFramebuffer();
    ctx.deleteFramebuffer(fbuf);
  }
}
