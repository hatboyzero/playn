/**
 * Copyright 2011 The PlayN Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package playn.java;

import java.awt.TexturePaint;
import java.awt.geom.Rectangle2D;

import static playn.core.PlayN.graphics;

import playn.core.Asserts;
import playn.core.Image;
import playn.core.ImageLayer;

class JavaImageLayer extends JavaLayer implements ImageLayer {

  private float width, height;
  private boolean widthSet, heightSet;
  private float sx, sy, sw, sh;
  private boolean repeatX, repeatY;

  private JavaImage image;
  private JavaCanvasImage cachedImage;
  private boolean dirty = true;

  JavaImageLayer() {
    super();
  }

  JavaImageLayer(JavaImage image) {
    super();
    this.image = image;
  }

  @Override
  public void clearHeight() {
    heightSet = false;
  }

  @Override @Deprecated
  public void clearSourceRect() {
    if (image instanceof Image.Region) {
      setImage(((Image.Region) image).parent());
    }
  }

  @Override
  public void clearWidth() {
    widthSet = false;
  }

  @Override
  public Image image() {
    return image;
  }

  @Override
  public void setHeight(float height) {
    Asserts.checkArgument(height > 0, "Height must be > 0");

    heightSet = true;
    if (this.height != height) {
      this.height = height;
      dirty = true;
    }
  }

  @Override
  public void setImage(Image image) {
    Asserts.checkArgument(image instanceof JavaImage);
    this.image = (JavaImage) image;
    dirty = true;
  }

  @Override
  public void setRepeatX(boolean repeat) {
    if (repeatX != repeat) {
      repeatX = repeat;
      dirty = true;
    }
  }

  @Override
  public void setRepeatY(boolean repeat) {
    if (repeatY != repeat) {
      repeatY = repeat;
      dirty = true;
    }
  }

  @Override @Deprecated
  public void setSourceRect(float sx, float sy, float sw, float sh) {
    Image source = (image instanceof Image.Region) ? ((Image.Region)image).parent() : image;
    setImage(source.subImage(sx, sy, sw, sh));
  }

  @Override
  public void setWidth(float width) {
    Asserts.checkArgument(width > 0, "Width must be > 0");

    widthSet = true;
    if (this.width != width) {
      this.width = width;
      dirty = true;
    }
  }

  @Override
  public void setSize(float width, float height) {
    Asserts.checkArgument(width > 0 && height > 0,
                          "Width and height must be > 0 (got %dx%d)", width, height);

    widthSet = true;
    if (this.width != width) {
      this.width = width;
      dirty = true;
    }
    heightSet = true;
    if (this.height != height) {
      this.height = height;
      dirty = true;
    }
  }

  @Override
  void paint(JavaCanvas canvas) {
    if (!visible() || !image.isReady()) return;

    canvas.save();
    transform(canvas);
    canvas.setAlpha(canvas.alpha() * alpha);

    float dw = width(), dh = height();
    if (repeatX || repeatY) {
      if (dirty) {
        // repaint repeated image onto cached image
        cachedImage = (JavaCanvasImage) graphics().createImage((int) dw, (int) dh);
        float anchorWidth = repeatX ? image.width() : dw;
        float anchorHeight = repeatY ? image.height() : dh;
        TexturePaint tpaint = ((JavaPattern) image.toPattern()).paint;
        ((JavaCanvas) cachedImage.canvas()).gfx.setPaint(tpaint);
        ((JavaCanvas) cachedImage.canvas()).gfx.fill(new Rectangle2D.Float(0, 0, dw, dh));
        dirty = false;
      }
      canvas.drawImage(cachedImage, 0, 0);
    } else if (widthSet || heightSet) {
      canvas.drawImage(image, 0, 0, dw, dh);
    } else {
      canvas.drawImage(image, 0, 0);
    }

    canvas.restore();
  }

  @Override
  public float width() {
    Asserts.checkNotNull(image, "Image must not be null");
    return widthSet ? width : image.width();
  }

  @Override
  public float height() {
    Asserts.checkNotNull(image, "Image must not be null");
    return heightSet ? height : image.height();
  }

  @Override
  public float scaledWidth() {
    return transform().scaleX() * width();
  }

  @Override
  public float scaledHeight() {
    return transform().scaleY() * height();
  }
}
