<!--   
  function openResource(path){

	 var locationFile = location.href;
	 var locationPath = locationFile.substring(0,locationFile.indexOf("/documents/en/"));
	 if(/{DOCHOME}/ig.test(path)){
		path = path.replace(/{DOCHOME}/ig,locationPath);
	 }
	
	openView(path);
  }
  function openView(url){

	  var  iWidth=600; 
	  var  iHeight=500;
	  var  iTop=(window.screen.height-iHeight)/2;
	  var  iLeft=(window.screen.width-iWidth)/2;
	 
      window.open(url,"Detail","Scrollbars=yes,Toolbar=no,Location=no,Direction=no,Resizeable=yes,Width="+iWidth+" ,Height="+iHeight+",top="+iTop+",left="+iLeft); 
  }
  //-->