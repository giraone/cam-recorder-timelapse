function loadImage(url, text) {
  document.getElementById('imageViewerIFrame').contentWindow.loadURL(url, text);
}