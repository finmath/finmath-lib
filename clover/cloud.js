function showDeepPackageCloud(button) {
    /* TODO toggling does not work */
    var show = ($(button).attr('aria-pressed') == undefined || $(button).attr('aria-pressed') == false);
	if (show) {
		document.getElementById('deepPackageCloud').style.display = 'block';
		document.getElementById('shallowPackageCloud').style.display = 'none';
	} else {
		document.getElementById('shallowPackageCloud').style.display = 'block';
		document.getElementById('deepPackageCloud').style.display = 'none';
	}
    $(button).attr('aria-pressed', show);
}