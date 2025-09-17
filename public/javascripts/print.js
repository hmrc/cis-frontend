(function () {
  var printButton = document.getElementById('print-button');
  if (!printButton) return;
  printButton.addEventListener('click', function (e) {
    e.preventDefault();
    window.print();
  });
}());