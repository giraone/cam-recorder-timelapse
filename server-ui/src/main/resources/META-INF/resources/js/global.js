function loadImage(url, text) {
  document.getElementById('imageViewerIFrame').contentWindow.loadUrl(url, text);
}
function loadVideo(url, text) {
  document.getElementById('videoViewerIFrame').contentWindow.loadUrl(url, text);
}