var highlightOn = true;

$(function(){
	for(i=0;i<queryterms.length;i++){
		str = queryterms[i];
		$('div.textStyle:contains('+str+')').
			each(function(){
				var regex = new RegExp("("+str+")(\\W)", "gi");
				$(this).html(
					$(this).html().
						replace( regex ,
							"<span class='highlight'>$1</span>$2"
							)
					);
				});
		
		$('pre.codeStyle:contains('+str+')').
		each(function(){
			var regex = new RegExp("("+str+")(\\W)", "gi");
			$(this).html(
				$(this).html().
					replace( regex ,
						"<span class='highlight'>$1</span>$2"
						)
				);
			});
		
	}
});

function turnOnOffHighlight(){
	if(highlightOn){
		highlightOn = false;
		$('.highlight').css({'background-color':'inherit'});
	}else{
		highlightOn = true;
		$('.highlight').css({'background-color':'yellow'});
	}
}

$(function(){
	$('#ToggleHighlightButton').click(turnOnOffHighlight);
});
