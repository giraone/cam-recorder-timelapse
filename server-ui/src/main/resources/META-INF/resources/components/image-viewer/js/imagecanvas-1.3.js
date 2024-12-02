const ImageCanvasPoint = function () {
    this.x = 0;
    this.y = 0;
};

ImageCanvasPoint.prototype.setXY = function (x, y) {
    this.x = x;
    this.y = y;
};

const ImageCanvasOptions = function () {
    // How fast is mouse panning done
    this.mousePanSpeed = 0.5;
    // How fast is scroll wheel zooming done
    this.mouseWheelSpeed = 0.33;
    // How fast is touch panning done
    this.touchPanSpeed = 1.0;
    // How fast is touch panning done
    this.twoFingerZoomSpeed = 1.0;
    // Double click within this time will zoom-in
    this.doubleClickMinLimitInMilliSeconds = 100;
    // Double click within this time will zoom-in
    this.doubleClickMaxLimitInMilliSeconds = 300;
    // Scale factor for relative scale and zoom (base for pow)
    this.scaleFactor = 1.1;
    // Brightness factor. 1 means 1 bit step (1-255)
    this.brightnessFactor = 8;
    // Contrast factor. Base for pow.
    this.contrastFactor = 1.1;

    // Info text size
    this.infoTextFont = "12px sans-serif";
    // Info text color
    this.infoTextColor = "#FFFFFF";
    // Info text position
    this.infoTextPosition = {"x": 10, "y": 14};

    // Draw boundary box
    this.drawBoundary = false;
    // Boundary size
    this.boundarySize = 10;
    // Boundary color
    this.boundaryColor = "#FFFFFF";

    this.msgLoadStart = "Loading {0} ...";
    this.msgLoaded = "Loaded {0}";
};

const FilterDefinition = function (filter, filterName, arg1, arg2, arg3) {
    this.filter = filter;
    this.filterName = filterName;
    this.arg1 = arg1;
    this.arg2 = arg2;
    this.arg3 = arg3;
};

const Filters = function (document, imageCanvas) {
    this.currentBrightness = 0.0;
    this.currentContrast = 1.0;
    this.imageCanvas = imageCanvas;
};

// @returns new empty ImageData object
Filters.prototype._createImageData = function (w, h) {
    const tmpCtx = document.createElement('canvas').getContext('2d');
    return tmpCtx.createImageData(w, h);
};

Filters.prototype._getPixels = function () {
    const ctx = this.imageCanvas.shadowCtx;
    return ctx.getImageData(0, 0, ctx.canvas.width, ctx.canvas.height);
};

// @returns an ImageData object
Filters.prototype._filterImage = function (filter, var_args) {
    const args = [this._getPixels()];
    for (let i = 2; i < arguments.length; i++) {
        args.push(arguments[i]);
    }
    return filter.apply(null, args);
};

Filters.prototype.grayscale = function (pixels) {
    const d = pixels.data;
    for (let i = 0; i < d.length; i += 4) {
        const r = d[i];
        const g = d[i + 1];
        const b = d[i + 2];
        // CIE luminance for the RGB
        const v = 0.2126 * r + 0.7152 * g + 0.0722 * b;
        d[i] = d[i + 1] = d[i + 2] = v;
    }
    return pixels;
};

Filters.prototype.redfree = function (pixels) {
    const d = pixels.data;
    for (let i = 0; i < d.length; i += 4) {
        const r = 0;
        const g = d[i + 1];
        const b = d[i + 2];
        // CIE luminance for the RGB
        const v = 0.2126 * r + 0.7152 * g + 0.0722 * b;
        d[i] = d[i + 1] = d[i + 2] = v;
    }
    return pixels;
};

Filters.prototype.brightness = function (pixels, adjustment) {
    const d = pixels.data;
    if (adjustment > 0) {
        for (let i = 0; i < d.length; i++) {
            d[i] += adjustment;
            if (d[i] > 255) {
                d[i] = 255;
            }
        }
    } else {
        for (let i = 0; i < d.length; i++) {
            d[i] += adjustment;
            if (d[i] < 0) {
                d[i] = 0;
            }
        }
    }
    return pixels;
};

Filters.prototype.contrast = function (pixels, adjustment) {
    const d = pixels.data;
    if (adjustment > 0) {
        for (let i = 0; i < d.length; i++) {
            d[i] = (d[i] - 128) * adjustment + 128;
            if (d[i] > 255) {
                d[i] = 255;
            }
        }
    } else {
        for (let i = 0; i < d.length; i++) {
            d[i] = (d[i] - 128) * adjustment + 128;
            if (d[i] < 0) {
                d[i] = 0;
            }
        }
    }
    return pixels;
};

Filters.prototype.convolute = function (pixels, weights, opaque) {
    const side = Math.round(Math.sqrt(weights.length));
    const halfSide = Math.floor(side / 2);

    const src = pixels.data;
    const sw = pixels.width;
    const sh = pixels.height;

    const output = this._createImageData(sw, sh);
    const dst = output.data;

    const alphaFac = opaque ? 1 : 0;

    for (let y = 0; y < sh; y++) {
        for (let x = 0; x < sw; x++) {
            const sy = y;
            const sx = x;
            const dstOff = (y * sw + x) * 4;
            let r = 0, g = 0, b = 0, a = 0;
            for (let cy = 0; cy < side; cy++) {
                for (let cx = 0; cx < side; cx++) {
                    const scy = Math.min(sh - 1, Math.max(0, sy + cy - halfSide));
                    const scx = Math.min(sw - 1, Math.max(0, sx + cx - halfSide));
                    const srcOff = (scy * sw + scx) * 4;
                    const wt = weights[cy * side + cx];
                    r += src[srcOff] * wt;
                    g += src[srcOff + 1] * wt;
                    b += src[srcOff + 2] * wt;
                    a += src[srcOff + 3] * wt;
                }
            }
            dst[dstOff] = r;
            dst[dstOff + 1] = g;
            dst[dstOff + 2] = b;
            dst[dstOff + 3] = a + alphaFac * (255 - a);
        }
    }
    return output;
};

Filters.prototype.convoluteFloat32 = function (pixels, weights, opaque) {
    const side = Math.round(Math.sqrt(weights.length));
    const halfSide = Math.floor(side / 2);

    const src = pixels.data;
    const sw = pixels.width;
    const sh = pixels.height;

    const output = {
        width: sw, height: sh, data: new Float32Array(sw * sh * 4)
    };
    const dst = output.data;

    const alphaFac = opaque ? 1 : 0;

    for (let y = 0; y < sh; y++) {
        for (let x = 0; x < sw; x++) {
            const sy = y;
            const sx = x;
            const dstOff = (y * sw + x) * 4;
            const r = 0, g = 0, b = 0, a = 0;
            for (let cy = 0; cy < side; cy++) {
                for (let cx = 0; cx < side; cx++) {
                    const scy = Math.min(sh - 1, Math.max(0, sy + cy - halfSide));
                    const scx = Math.min(sw - 1, Math.max(0, sx + cx - halfSide));
                    const srcOff = (scy * sw + scx) * 4;
                    const wt = weights[cy * side + cx];
                    r += src[srcOff] * wt;
                    g += src[srcOff + 1] * wt;
                    b += src[srcOff + 2] * wt;
                    a += src[srcOff + 3] * wt;
                }
            }
            dst[dstOff] = r;
            dst[dstOff + 1] = g;
            dst[dstOff + 2] = b;
            dst[dstOff + 3] = a + alphaFac * (255 - a);
        }
    }
    return output;
};

//----------------------------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------------------------

const ImageCanvasInitOnce = function () {
};

ImageCanvasInitOnce.prototype.disableDefaultEventsOnDocument = function (document) {
    const preventDefaultTouchBehavior = function (e) {
        e.preventDefault();
        return false;
    };

    if ('ontouchmove' in window) {
        // do not prevent start and end, otherwise button and menu clicks won't work
        document.addEventListener("touchmove", preventDefaultTouchBehavior, false);
    }
};

ImageCanvasInitOnce.prototype.disableMouseWheelOnDocument = function (document) {
    const cancelScroll = function (e) {
        e.preventDefault();
    };

    if ("onmousewheel" in document) {
        document.onmousewheel = cancelScroll;
    } else {
        document.addEventListener('DOMMouseScroll', cancelScroll, false);
    }
};

//----------------------------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------------------------

const ImageCanvas = function (document, canvas, logFunction) {
    this.options = new ImageCanvasOptions();
    this.ctx = canvas.getContext('2d');

    // A shadow canvas that has the exact dimensions of the image.
    // All image manipulation, that change image data (pixels) is done on this canvas
    this.shadowCtx = document.createElement('canvas').getContext('2d');

    this.logFunction = logFunction;

    // For transformations based on matrix
    this.svg = document.createElementNS("http://www.w3.org/2000/svg", 'svg');
    this.xform = this.svg.createSVGMatrix();
    this.svgPoint = this.svg.createSVGPoint();
    this.savedTransforms = [];

    this.lastX = this.ctx.canvas.width / 2;
    this.lastY = this.ctx.canvas.height / 2;
    this.panStartPoint = null;
    // is panning started
    this.isPanningStarted = false;
    // For the last two finger distance
    this.lastTwoFingerDistance = null;
    // For "double click"
    this.lastClickEndTime = null;

    //----------------------------------------------------------------------------
    // Touch and Mouse Events
    //----------------------------------------------------------------------------

    let xThis = this;

    if ('ontouchstart' in window) {
        this.logFunction("ontouchstart available");
        this.ctx.canvas.ontouchstart = function (e) {
            return xThis.ontouchstart(e);
        };
        this.ctx.canvas.ontouchmove = function (e) {
            return xThis.ontouchmove(e);
        };
        this.ctx.canvas.ontouchend = function (e) {
            return xThis.ontouchend(e);
        };

        this.ctx.canvas.addEventListener('touchstart', this.ctx.canvas.ontouchstart, false);
        this.ctx.canvas.addEventListener('touchmove', this.ctx.canvas.ontouchmove, /* useCapture */ true);
        this.ctx.canvas.addEventListener('touchend', this.ctx.canvas.ontouchend, false);

        document.body.addEventListener("touchcancel", this.ctx.canvas.ontouchend, false);
    }

    // Chrome has both on Win8: ontouchstart and onmousedown, therefore no if/else
    if ('onmousedown' in window) {
        this.logFunction("onmousedown available");
        this.ctx.canvas.onmousedown = function (e) {
            return xThis.onmousedown(e);
        };
        this.ctx.canvas.onmousemove = function (e) {
            return xThis.onmousemove(e);
        };
        this.ctx.canvas.onmouseup = function (e) {
            return xThis.onmouseup(e);
        };
        const MouseScroll = function (e) {
            return xThis.onmousewheel(e);
        };

        if (this.ctx.canvas.addEventListener) {
            // Mozilla, Safari, Chrome
            this.logFunction("addEventListener available");
            this.ctx.canvas.addEventListener('DOMMouseScroll', MouseScroll, false);
            this.ctx.canvas.addEventListener('mousewheel', MouseScroll, false);
        } else if ("onmousewheel" in this.ctx.canvas) {
            // Safari, Chrome
            this.logFunction("onmousewheel available");
            this.ctx.canvas.onmousewheel = this.onmousewheel;
        } else if (this.ctx.canvas.attachEvent) {
            // IE before version 9
            this.logFunction("attachEvent available");
            this.ctx.canvas.attachEvent("onmousewheel", MouseScroll);
        }

        document.body.addEventListener("mouseup", this.ctx.canvas.onmouseup, false);
    }

    //----------------------------------------------------------------------------

    this.filters = new Filters(document, this);

    this.currentImage = null;
    this.currentFilterDefinition = null;
    this.currentScale = 1.0;

    this.zoomCallback = this.brightCallback = this.contrastCallback = function (value) {
    };
    this.doubleClickCallback = null;
    this.loadedCallback = null;

    return this;
};

//----------------------------------------------------------------------------------------------------------------------
//- XFORM STUFF --------------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------------------------

ImageCanvas.prototype.getTransformX = function () {
    return this.xform;
};

ImageCanvas.prototype.saveX = function () {
    this.savedTransforms.push(this.xform);
};

ImageCanvas.prototype.restoreX = function () {
    const saved = this.savedTransforms.pop();
    if (saved != null) {
        this.xform = saved;
    }
};

ImageCanvas.prototype.scaleX = function (sx, sy) {
    this.xform = this.xform.scaleNonUniform(sx, sy);
    this.currentScale = this.currentScale * sx;
    //this._log("scaleX " + sx + " => " + this.currentScale);
    return this.ctx.scale(sx, sy);
};

ImageCanvas.prototype.rotateX = function (radian) {
    this.xform = this.xform.rotate(radian * 180 / Math.PI);
    return this.ctx.rotate(radian);
};

ImageCanvas.prototype.translateX = function (dx, dy) {
    this.xform = this.xform.translate(dx, dy);
    return this.ctx.translate(dx, dy);
};

ImageCanvas.prototype.transformX = function (a, b, c, d, e, f) {
    const m2 = this.svg.createSVGMatrix();
    m2.a = a;
    m2.b = b;
    m2.c = c;
    m2.d = d;
    m2.e = e;
    m2.f = f;
    this.xform = this.xform.multiply(m2);
    this.currentScale = this.currentScale * a;
    this._log("transformX " + a + " => " + this.currentScale);
    return this.ctx.transform(a, b, c, d, e, f);
};

ImageCanvas.prototype.setTransformX = function (a, b, c, d, e, f) {
    this.xform.a = a;
    this.xform.b = b;
    this.xform.c = c;
    this.xform.d = d;
    this.xform.e = e;
    this.xform.f = f;
    this.currentScale = this.currentScale * a;
    //this._log("setTransformX " + a + " => " + this.currentScale);
    return this.ctx.setTransform(a, b, c, d, e, f);
};

ImageCanvas.prototype.transformedPointX2 = function (x, y) {
    this.svgPoint.x = x;
    this.svgPoint.y = y;
    return this.svgPoint.matrixTransform(this.xform);
};

ImageCanvas.prototype.transformedPointX = function (x, y) {
    this.svgPoint.x = x;
    this.svgPoint.y = y;
    return this.svgPoint.matrixTransform(this.xform.inverse());
};

//----------------------------------------------------------------------------------------------------------------------
//- PRIVATE HELPER -----------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------------------------

ImageCanvas.prototype._log = function (msg) {
    if (this.logFunction !== null) {
        this.logFunction(msg);
    }
};

ImageCanvas.prototype._onLoadNewFile = function (newImage, fit) {
    //this._log("_onLoadNewFile " + newImage.src);
    this._setNewImage(newImage);

    if (fit) {
        this._setScaleToFit();
    }
    this._redrawFromShadow();
    this._setInfoText(this.options.msgLoaded.replace(/\{0}/, newImage.src));
    this.zoomCallback(this.currentScale);
    if (this.loadedCallback != null)
        this.loadedCallback();
};

ImageCanvas.prototype._setNewImage = function (image) {
    this.currentImage = image;
    this._reloadCurrentImage();
};

ImageCanvas.prototype._reloadCurrentImage = function () {
    this.shadowCtx.canvas.width = this.currentImage.width;
    this.shadowCtx.canvas.height = this.currentImage.height;
    this.shadowCtx.drawImage(this.currentImage, 0, 0);
};

ImageCanvas.prototype._redrawFromShadow = function () {
    // Clear the entire canvas
    this.clearImage();
    // Draw from the shadow canvas
    this.ctx.drawImage(this.shadowCtx.canvas,
        0, 0,
        this.currentImage.width, this.currentImage.height,
        0, 0,
        this.currentImage.width, this.currentImage.height
    );
    if (this.options.drawBoundary) {
        this._drawBoundary();
    }
};

ImageCanvas.prototype._setInfoText = function (text) {
    this.ctx.save();
    this.ctx.setTransform(1, 0, 0, 1, 0, 0);
    this.ctx.font = this.options.infoTextFont;
    this.ctx.fillStyle = this.options.infoTextColor;
    this.ctx.fillText(text, this.options.infoTextPosition.x, this.options.infoTextPosition.y);
    this.ctx.restore();
};

/**
 * Draw a small boundary around the image to see the image box on the black background.
 * @private
 */
ImageCanvas.prototype._drawBoundary = function () {
    const p0 = this.transformedPointX2(this.options.boundarySize, this.options.boundarySize);
    const p1 = this.transformedPointX2(-this.options.boundarySize, -this.options.boundarySize);
    const p2 = this.transformedPointX2(this.currentImage.width + this.options.boundarySize, this.currentImage.height + this.options.boundarySize);

    this._log(Math.abs(p1.x - p0.x) + " " + p1.x + " " + p1.y + " " + (p2.x - p1.x) + " " + (p2.y - p1.y));

    this.ctx.strokeStyle = this.options.boundaryColor;
    this.ctx.lineWidth = this.options.boundarySize;
    this.ctx.strokeRect(p1.x, p1.y, p2.x - p1.x, p2.y - p1.y);
};

ImageCanvas.prototype._runFilter = function (filter, filterName, store, redraw, arg1, arg2, arg3) {
    //this._log("ImageCanvas._runFilter filterName=" + filterName + " store=" + store + " redraw=" + redraw + " args=" + arg1 + " " + arg2 + " " + arg3);
    const imageData = this.filters._filterImage(filter, arg1, arg2, arg3);
    this.shadowCtx.putImageData(imageData, 0, 0);
    if (redraw) {
        this._redrawFromShadow();
    }
    //this.ctx.canvas.style.display = 'inline';
    if (store) {
        this.currentFilterDefinition = new FilterDefinition(filter, filterName, arg1, arg2, arg3);
    }
};

ImageCanvas.prototype._applyCurrentFilter = function (redraw) {
    //this._log("ImageCanvas.applyCurrentFilter " + redraw);
    if (this.currentFilterDefinition != null)
        this._runFilter(this.currentFilterDefinition.filter, this.currentFilterDefinition.filterName, true, redraw,
            this.currentFilterDefinition.arg1, this.currentFilterDefinition.arg2, this.currentFilterDefinition.arg3);
};

ImageCanvas.prototype._resetView = function () {
    this.setTransformX(1, 0, 0, 1, 0, 0);
};

ImageCanvas.prototype._resetCurrent = function () {
    this.filters.currentBrightness = 0.0;
    this.filters.currentContrast = 1.0;
};

ImageCanvas.prototype._setScaleToFit = function () {
    if (this.currentImage == null) return;
    const iWidth = this.currentImage.width;
    const iHeight = this.currentImage.height;
    const cWidth = this.ctx.canvas.width;
    const cHeight = this.ctx.canvas.height;
    const xScale = cWidth / iWidth;
    const yScale = cHeight / iHeight;
    let newScale;
    if (xScale > yScale) {
        newScale = yScale;
    } else {
        newScale = xScale;
    }
    this.setTransformX(newScale, 0, 0, newScale, 0, 0);
};

//----------------------------------------------------------------------------------------------------------------------
//- PRIVATE MOUSE LISTENER ---------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------------------------

ImageCanvas.prototype._onmove = function (x, y, speed) {
    if (this.panStartPoint != null) {
        const pt = this.transformedPointX(this.lastX, this.lastY);
        this.translateX((pt.x - this.panStartPoint.x) * speed, (pt.y - this.panStartPoint.y) * speed);
        this._redrawFromShadow();
    }
};

ImageCanvas.prototype.onmousedown = function (e) {
    document.body.style.mozUserSelect = document.body.style.webkitUserSelect = document.body.style.userSelect = 'none';
    this.lastX = e.offsetX || (e.pageX - this.ctx.canvas.offsetLeft);
    this.lastY = e.offsetY || (e.pageY - this.ctx.canvas.offsetTop);
    this.panStartPoint = this.transformedPointX(this.lastX, this.lastY);
    this.isPanningStarted = false;
};

ImageCanvas.prototype.onmousemove = function (e) {
    this.lastX = e.offsetX || (e.pageX - this.ctx.canvas.offsetLeft);
    this.lastY = e.offsetY || (e.pageY - this.ctx.canvas.offsetTop);
    this.isPanningStarted = true;
    this._onmove(this.lastX, this.lastY, this.options.mousePanSpeed);
};

ImageCanvas.prototype.onmouseup = function (e) {
    this.panStartPoint = null;
    const ms = new Date().getTime();
    if (!this.isPanningStarted && this.lastClickEndTime != null) {
        const diff = ms - this.lastClickEndTime;
        if (diff < this.options.doubleClickMaxLimitInMilliSeconds) {
            if (diff > this.options.doubleClickMinLimitInMilliSeconds) {
                //this._log("doubleClickCallback onmouseup " + diff);
                if (this.doubleClickCallback != null)
                    this.doubleClickCallback();
                else
                    this.zoomRelative(e.shiftKey ? -1 : 1);
                this.lastClickEndTime = null;
            } else {
                this.lastClickEndTime = ms;
            }
        } else {
            this.lastClickEndTime = null;
        }
    } else {
        this.lastClickEndTime = ms;
    }
};

ImageCanvas.prototype.onmousewheel = function (e) {
    const delta = e.wheelDelta ? e.wheelDelta / 40 : e.detail ? -e.detail : 0;
    if (delta) {
        this.zoomRelative(delta * this.options.mouseWheelSpeed);
    }
    return e.preventDefault() && false;
};

//----------------------------------------------------------------------------------------------------------------------
//- PRIVATE TOUCH LISTENER ---------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------------------------

ImageCanvas.prototype.ontouchstart = function (e) {
    this.lastX = e.targetTouches[0].pageX - this.ctx.canvas.offsetLeft;
    this.lastY = e.targetTouches[0].pageY - this.ctx.canvas.offsetTop;
    if (e.targetTouches.length > 1) {
        const lastX2 = e.targetTouches[1].pageX - this.ctx.canvas.offsetLeft;
        const lastY2 = e.targetTouches[1].pageY - this.ctx.canvas.offsetTop;
        this.lastTwoFingerDistance = Math.sqrt((lastX2 - this.lastX) * (lastX2 - this.lastX) + (lastY2 - this.lastY) * (lastY2 - this.lastY));
    } else {
        this.lastTwoFingerDistance = null;
        this.panStartPoint = this.transformedPointX(this.lastX, this.lastY);
        this.isPanningStarted = false;
    }
    return e.preventDefault() && false;
};

ImageCanvas.prototype.ontouchmove = function (e) {
    this.lastX = e.targetTouches[0].pageX - this.ctx.canvas.offsetLeft;
    this.lastY = e.targetTouches[0].pageY - this.ctx.canvas.offsetTop;

    if (e.targetTouches.length > 1) {
        const lastX2 = e.targetTouches[1].pageX - this.ctx.canvas.offsetLeft;
        const lastY2 = e.targetTouches[1].pageY - this.ctx.canvas.offsetTop;
        const diffXY2 = Math.sqrt((lastX2 - this.lastX) * (lastX2 - this.lastX) + (lastY2 - this.lastY) * (lastY2 - this.lastY));

        if (this.lastTwoFingerDistance != null) {
            this.zoomRelative((diffXY2 - this.lastTwoFingerDistance) / 30.0 * this.options.twoFingerZoomSpeed);
            this.lastTwoFingerDistance = diffXY2;
        } else {
            this.lastTwoFingerDistance = diffXY2;
        }
    } else {
        this.isPanningStarted = true;
        this._onmove(this.lastX, this.lastY, this.options.touchPanSpeed);
    }
    return e.preventDefault() && false;
};

ImageCanvas.prototype.ontouchend = function (e) {
    const ms = new Date().getTime();
    if (!this.isPanningStarted && this.lastClickEndTime != null) {
        const diff = ms - this.lastClickEndTime;
        if (diff < this.options.doubleClickMaxLimitInMilliSeconds) {
            if (diff > this.options.doubleClickMinLimitInMilliSeconds) {
                //this._log("doubleClickCallback ontouchend " + diff);
                if (this.doubleClickCallback != null)
                    this.doubleClickCallback();
                else
                    this.zoomRelative(1);
                this.lastClickEndTime = null;
            } else {
                this.lastClickEndTime = ms;
            }
        } else {
            this.lastClickEndTime = null;
        }
    } else {
        this.lastClickEndTime = ms;
    }

    this.isPanningStarted = false;

    return e.preventDefault() && false;
};

//----------------------------------------------------------------------------------------------------------------------
//- PUBLIC METHODS -----------------------------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------------------------

ImageCanvas.prototype.clearImage = function () {
    const p1 = this.transformedPointX(0, 0);
    const p2 = this.transformedPointX(this.ctx.canvas.width, this.ctx.canvas.height);
    // faster than fillRect with black
    this.ctx.clearRect(p1.x, p1.y, p2.x - p1.x, p2.y - p1.y);
};

ImageCanvas.prototype.scaleToContainer = function () {
    this.lastX = this.ctx.canvas.width / 2;
    this.lastY = this.ctx.canvas.height / 2;
    this._setScaleToFit();
    this._redrawFromShadow();
};

ImageCanvas.prototype.loadImage = function (url, fit, text) {
    //this._log("loadImage " + filename);
    const newImage = new Image();

    this._resetCurrent();
    this._resetView();
    this.clearImage();

    if (text)
        this._setInfoText(text);
    else
        this._setInfoText(this.options.msgLoadStart.replace(/\{0}/, url));

    const xThis = this;
    newImage.onload = function () {
        xThis._onLoadNewFile(newImage, fit);
        newImage.onload = null;
    };
    newImage.src = url;
};

ImageCanvas.prototype.showImageInfo = function () {
    let s = "\r\n";
    if (this.currentImage !== null) {
        s += "\r\nImage     : " + this.currentImage.src;
        s += "\r\nDimension : " + this.currentImage.width + "*" + this.currentImage.height;
    } else {
        s += "\r\nImage     : NONE";
    }
    if (this.ctx && this.ctx.canvas) {
        s += "\r\nCanvas    : " + this.ctx.canvas.width + "*" + this.ctx.canvas.height;
    }
    alert(s);
};

ImageCanvas.prototype.scaleAbsolute = function (newScale) {
    //this._log("ImageCanvas.scaleAbsolute " + newScale);
    this._resetView();
    this.scaleX(newScale, newScale);
    this.currentScale = newScale;
    if (newScale === 1.0) {
        const iWidth = this.currentImage.width;
        const iHeight = this.currentImage.height;
        const cWidth = this.ctx.canvas.width;
        const cHeight = this.ctx.canvas.height;
        this._log(`ImageCanvas.scaleAbsolute: Image=${iWidth}x${iHeight} Canvas=${cWidth}x${cHeight}`);
        if (iWidth > cWidth) {
            const dx = - (iWidth - cWidth) / 2;
            const dy = - (iHeight - cHeight) / 2;
            this._log(`ImageCanvas.scaleAbsolute: Translate=${dx}x${dy}`);
            this.translateX(dx, dy)
        }
    }
    this._redrawFromShadow();
    this.zoomCallback(this.currentScale);
};

ImageCanvas.prototype.scaleRelative = function (powValue) {
    //this._log("ImageCanvas.scaleRelative " + powValue);
    const factor = Math.pow(this.options.scaleFactor, powValue);
    this.scaleX(factor, factor);
    this._redrawFromShadow();
    this.zoomCallback(this.currentScale);
};

ImageCanvas.prototype.zoomAbsolute = function (newScale) {
    //this._log("ImageCanvas.zoomAbsolute " + newScale);
    let pt = this.transformedPointX(this.lastX, this.lastY);
    this.translateX(pt.x, pt.y);
    this.currentScale = newScale;
    this.scaleX(newScale, newScale);
    this.translateX(-pt.x, -pt.y);
    this._redrawFromShadow();
    this.zoomCallback(this.currentScale);
};

ImageCanvas.prototype.zoomRelative = function (powValue) {
    //this._log("ImageCanvas.zoomRelative " + powValue);
    const pt = this.transformedPointX(this.lastX, this.lastY);
    this.translateX(pt.x, pt.y);
    const factor = Math.pow(this.options.scaleFactor, powValue);
    this.scaleX(factor, factor);
    this.translateX(-pt.x, -pt.y);
    this._redrawFromShadow();
    this.zoomCallback(this.currentScale);
};

ImageCanvas.prototype.scaleToFit = function () {
    this._setScaleToFit();
    this._redrawFromShadow();
    this.zoomCallback(this.currentScale);
};

//----------------------------------------------------------------------------------------------------------------------
//- Image manipulation applied additive. -------------------------------------------------------------------------------
//----------------------------------------------------------------------------------------------------------------------

ImageCanvas.prototype.original = function () {
    //this._log("ImageCanvas.original");
    this._reloadCurrentImage();
    this._redrawFromShadow();
};

ImageCanvas.prototype.brightnessAbsolute = function (newValue) {
    //this._log("ImageCanvas.brightnessAbsolute " + newValue);
    this._reloadCurrentImage();
    this._applyCurrentFilter(false);
    const thisX = this;
    this._runFilter(function (px) {
        return thisX.filters.brightness(px, newValue);
    }, "brightness", false, true);
    this.filters.currentBrightness = newValue;
    this.brightCallback(this.filters.currentBrightness / this.options.brightnessFactor);
};

ImageCanvas.prototype.brightnessRelative = function (value) {
    let delta;
    let newValue;
    if (value > 0) {
        if (this.filters.currentBrightness >= 0) {
            // Fast change without reload
            delta = this.options.brightnessFactor * value;
            newValue = this.filters.currentBrightness + delta;
        } else {
            this._reloadCurrentImage();
            this._applyCurrentFilter(false);
            delta = newValue = this.filters.currentBrightness + this.options.brightnessFactor * value;
        }
    } else {
        if (this.filters.currentBrightness <= 0) {
            // Fast change without reload
            delta = this.options.brightnessFactor * value;
            newValue = this.filters.currentBrightness + delta;
        } else {
            this._reloadCurrentImage();
            this._applyCurrentFilter(false);
            delta = newValue = this.filters.currentBrightness + this.options.brightnessFactor * value;
        }
    }
    const thisX = this;
    this._runFilter(function (px) {
        return thisX.filters.brightness(px, delta);
    }, "brightness", false, true);
    this.filters.currentBrightness = newValue;
    //this._log("ImageCanvas.brightnessRelative currentBrightness = " + this.filters.currentBrightness);
    this.brightCallback(this.filters.currentBrightness / this.options.brightnessFactor);
};

ImageCanvas.prototype.contrastAbsolute = function (newValue) {
    //this._log("ImageCanvas.contrastAbsolute " + newValue);
    this._reloadCurrentImage();
    this._applyCurrentFilter(false);
    const thisX = this;
    this._runFilter(function (px) {
        return thisX.filters.contrast(px, newValue);
    }, "contrast", false, true);
    this.filters.currentContrast = newValue;
    this.contrastCallback(this.filters.currentContrast);
};

ImageCanvas.prototype.contrastRelative = function (powValue) {
    let factor;
    let newValue;
    if (powValue > 0) {
        if (this.filters.currentContrast >= 1.0) {
            // Fast change without reload
            factor = Math.pow(this.options.contrastFactor, powValue);
            newValue = this.filters.currentContrast * factor;
            //this._log("ImageCanvas.contrastRelative a " + powValue + " " + newValue);
        } else {
            this._reloadCurrentImage();
            this._applyCurrentFilter();
            factor = newValue = this.filters.currentContrast * Math.pow(this.options.contrastFactor, powValue);
            //this._log("ImageCanvas.contrastRelative b " + powValue + " " + newValue);
        }
    } else {
        if (this.filters.currentContrast <= 1.0) {
            // Fast change without reload
            factor = Math.pow(this.options.contrastFactor, powValue);
            newValue = this.filters.currentContrast * factor;
            //this._log("ImageCanvas.contrastRelative c " + powValue + " " + newValue);
        } else {
            this._reloadCurrentImage();
            this._applyCurrentFilter();
            factor = newValue = this.filters.currentContrast * Math.pow(this.options.contrastFactor, powValue);
            //this._log("ImageCanvas.contrastRelative d " + powValue + " " + newValue);
        }
    }
    const thisX = this;
    this._runFilter(function (px) {
        return thisX.filters.contrast(px, factor);
    }, "contrast", false, true);
    this.filters.currentContrast = newValue;
    //this._log("ImageCanvas.currentContrast currentContrast = " + this.filters.currentContrast);
    this.contrastCallback(Math.log(this.filters.currentContrast) / Math.log(this.options.contrastFactor));
};

ImageCanvas.prototype.sharpen = function (value) {
    //this._log("ImageCanvas.sharpen " + value);
    const thisX = this;
    this._runFilter(function (px) {
        return thisX.filters.convolute(px,
            [0, -1, 0,
                -1, value, -1,
                0, -1, 0], /* opaque */ false);
    }, "sharpen", false, true);
};

ImageCanvas.prototype.sharpenNormal = function () {
    //this._log("ImageCanvas.sharpenNormal");
    this.sharpen(5);
};

//----------------------------------------------------------------------------------------------------------------------
//- Image manipulation applied on the original image data --------------------------------------------------------------
//----------------------------------------------------------------------------------------------------------------------

ImageCanvas.prototype.grayscale = function () {
    //this._log("ImageCanvas.grayscale");
    this._reloadCurrentImage();
    this._runFilter(this.filters.grayscale, "grayscale", true, true);
};

ImageCanvas.prototype.redfree = function () {
    //this._log("ImageCanvas.redfree");
    this._reloadCurrentImage();
    this._runFilter(this.filters.redfree, "redfree", true, true);
};

ImageCanvas.prototype.sobel = function () {
    this._log("ImageCanvas.sobel");
    this._reloadCurrentImage();
    const thisX = this;
    this._runFilter(function (px) {
        px = thisX.filters.grayscale(px);
        const vertical = thisX.filters.convoluteFloat32(px,
            [-1, -2, -1,
                0, 0, 0,
                1, 2, 1], /* opaque */ false);
        const horizontal = thisX.filters.convoluteFloat32(px,
            [-1, 0, 1,
                -2, 0, 2,
                -1, 0, 1], /* opaque */ false);
        const imageData = thisX.filters._createImageData(vertical.width, vertical.height);
        for (let i = 0; i < imageData.data.length; i += 4) {
            const v = Math.abs(vertical.data[i]);
            imageData.data[i] = v;
            const h = Math.abs(horizontal.data[i]);
            imageData.data[i + 1] = h;
            imageData.data[i + 2] = (v + h) / 4;
            imageData.data[i + 3] = 255;
        }
        return imageData;
    }, "sobel", true);
};

ImageCanvas.prototype.custom = function () {
    this._log("ImageCanvas.custom");
    this._reloadCurrentImage();
    const thisX = this;
    this._runFilter(function (px) {
        return thisX.filters.convolute(px,
            [1.0, 1.0, 1.0,
                1.0, 0.7, -1.0,
                -1.0, -1.0, -1.0], /* opaque */ false);
    }, "custom", true, true);
};
