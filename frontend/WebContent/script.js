/*Script for toggle advance features*/
$(function(){
	$("#advFeaturesToggleButton").click(function (){
		$("#advanceFeatures").slideToggle("slow",function(){});
	});
});

/*Script for select code snippet in element a*/
function selectCode(a)
{
	// Get ID of code block
	var e = a.parentNode.getElementsByTagName('PRE')[0];
 
	// Not IE
	if (window.getSelection)
	{
		var s = window.getSelection();
		// Safari
		if (s.setBaseAndExtent)
		{
			s.setBaseAndExtent(e, 0, e, e.innerText.length - 1);
		}
		// Firefox and Opera
		else
		{
			var r = document.createRange();
			r.selectNodeContents(e);
			s.removeAllRanges();
			s.addRange(r);
		}
	}
	// Some older browsers
	else if (document.getSelection)
	{
		var s = document.getSelection();
		var r = document.createRange();
		r.selectNodeContents(e);
		s.removeAllRanges();
		s.addRange(r);
	}
	// IE
	else if (document.selection)
	{
		var r = document.body.createTextRange();
		r.moveToElementText(e);
		r.select();
	}
	
	return false;
}

//dim the screen
$(function() {
    $("pre.codeStyle, .resultHeader").click(function(){
    		var codeResultBoxHeader=$(this).parent().children('.resultHeader');
    		var codeResultBox=$(this).parent().children('pre.codeStyle');
    		var page_url = $(this).siblings('a.urlLink').text();
    		$('#databoxHeaderWord').text(codeResultBoxHeader.text());
    		$('#databox pre').html(codeResultBox.html());
    		$('#databox pre').scrollTop(0);
    		$('#urlLinkBigScreen').attr('href',page_url).text(page_url);
        	$('#lightbox').fadeIn('fast',function(){
	            $('#databox').animate({'top':'100px'},500);
	        });
        	return false;
    	});
    
    $('#lightbox,#closebox').click(function(){
        $('#databox').animate({'top':'-500px'},500,function(){
            $('#lightbox').fadeOut('fast');
        });
        return false;
    });
});
