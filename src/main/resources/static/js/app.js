(function($) {
  'use strict';

  $(function() {
    var $fullText = $('.admin-fullText');
    $('#admin-fullscreen').on('click', function() {
      $.AMUI.fullscreen.toggle();
    });

    $(document).on($.AMUI.fullscreen.raw.fullscreenchange, function() {
      $fullText.text($.AMUI.fullscreen.isFullscreen ? '退出全屏' : '开启全屏');
    });
  });
})(jQuery);

//图片选择上传
function capp() {
	var r = new FileReader();
	f = document.getElementById('fileapp').files[0];
	r.readAsDataURL(f);
	r.onload = function(e) {
		document.getElementById('showapp').src = this.result;
	};
}