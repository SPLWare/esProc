<!--

//MzTreeView1.0网页树类, 在实例化的时候请把实例名作参数传递进来
function MzTreeView(Tname)
{
  if(typeof(Tname) != "string" || Tname == "")
    throw(new Error(-1, '创建类实例的时候请把类实例的引用变量名传递进来！'));
  
  //【property】
  this.url      = "#";
  this.target   = "_self";
  this.name     = Tname;
  this.wordLine = false;
  this.currentNode = null;
  this.useArrow = true;
  this.nodes = {};
  this.node  = {};
  this.names = "";
  this._d    = "\x0f";
  this.index = 0;
  this.divider   = "_";
  this.node["0"] =
  {
    "id": "0",
    "path": "0",
    "isLoad": false,
    "childNodes": [],
    "childAppend": "",
    "sourceIndex": "0"
  };

  this.colors   =
  {
    "highLight" : "#3885BE",
    "highLightText" : "#FFFFFF",
    "mouseOverBgColor" : "#D4D0C8"
  };
  this.icons    = {
    L0        : 'L0.gif',  //┏
    L1        : 'L1.gif',  //┣
    L2        : 'L2.gif',  //┗
    L3        : 'L3.gif',  //━
    L4        : 'L4.gif',  //┃
    PM0       : 'P0.gif',  //＋┏
    PM1       : 'P1.gif',  //＋┣
    PM2       : 'P2.gif',  //＋┗
    PM3       : 'P3.gif',  //＋━
    empty     : 'L5.gif',     //空白图
    root      : 'root.gif',   //缺省的根节点图标
    folder    : 'folder.gif', //缺省的文件夹图标
    file      : 'file.gif',   //缺省的文件图标
    exit      : 'exit.gif'
  };
  this.iconsExpand = {  //存放节点图片在展开时的对应图片
    PM0       : 'M0.gif',     //－┏
    PM1       : 'M1.gif',     //－┣
    PM2       : 'M2.gif',     //－┗
    PM3       : 'M3.gif',     //－━
    folder    : 'folderopen.gif',

    exit      : 'exit.gif'
  };

  //扩展 document.getElementById(id) 多浏览器兼容性
  //id 要查找的对象 id
  this.getElementById = function(id)
  {
    if (typeof(id) != "string" || id == "") return null;
    if (document.getElementById) return document.getElementById(id);
    if (document.all) return document.all(id);
    try {return eval(id);} catch(e){ return null;}
  }

  //MzTreeView 初始化入口函数
  this.toString = function()
  {
    this.browserCheck();
    this.dataFormat();
    this.setStyle();
    this.load("0");
    var rootCN = this.node["0"].childNodes;
    var str = "<A id='"+ this.name +"_RootLink' href='#' style='DISPLAY: none'></A>";
    
    if(rootCN.length>0)
    {
      this.node["0"].hasChild = true;
      for(var i=0; i<rootCN.length; i++)
        str += this.nodeToHTML(rootCN[i], i==rootCN.length-1);
      setTimeout(this.name +".expand('"+ rootCN[0].id +"', true); "+ 
        this.name +".focusClientNode('"+ rootCN[0].id +"'); "+ this.name +".atRootIsEmpty()",10);
    }

    if (this.useArrow)  //使用方向键控制跳转到上级下级父级子级节点
    {
      if (document.attachEvent)
          document.attachEvent("onkeydown", this.onkeydown);
      else if (document.addEventListener)
          document.addEventListener('keydown', this.onkeydown, false);
    }
    return "<DIV class='MzTreeView' "+
      "onclick='"+ this.name +".clickHandle(event)' "+
      "ondblclick='"+ this.name +".dblClickHandle(event)' "+
      ">"+ str +"</DIV>";
  };

  this.onkeydown= function(e)
  {
    e = window.event || e; var key = e.keyCode || e.which;
    switch(key)
    {
      case 37 : eval(Tname).upperNode(); break;  //Arrow left, shrink child node
      case 38 : eval(Tname).pervNode();  break;  //Arrow up
      case 39 : eval(Tname).lowerNode(); break;  //Arrow right, expand child node
      case 40 : eval(Tname).nextNode();  break;  //Arrow down
    }
  };
}

//浏览器类型及版本检测
MzTreeView.prototype.browserCheck = function()
{
  var ua = window.navigator.userAgent.toLowerCase(), bname;
  if(/msie/i.test(ua))
  {
    this.navigator = /opera/i.test(ua) ? "opera" : "";
    if(!this.navigator) this.navigator = "msie";
  }
  else if(/gecko/i.test(ua))
  {
    var vendor = window.navigator.vendor.toLowerCase();
    if(vendor == "firefox") this.navigator = "firefox";
    else if(vendor == "netscape") this.navigator = "netscape";
    else if(vendor == "") this.navigator = "mozilla";
  }
  else this.navigator = "msie";
  if(window.opera) this.wordLine = false;
};

//给 TreeView 树加上样式设置
MzTreeView.prototype.setStyle = function()
{
  /*
    width: 16px; \
    height: 16px; \
    width: 20px; \
    height: 20px; \
  */
  var style = "<style>"+
  "DIV.MzTreeView DIV IMG{border: 0px solid #FFFFFF;}"+
  "DIV.MzTreeView DIV SPAN IMG{border: 0px solid #FFFFFF;}";
  //if(this.wordLine)
  if(false)
  {
    style +="\
    DIV.MzTreeView DIV\
    {\
      height: 20px;"+
      (this.navigator=="firefox" ? "line-height: 20px;" : "" ) +
      (this.navigator=="netscape" ? "" : "overflow: hidden;" ) +"\
    }\
    DIV.MzTreeView DIV SPAN\
    {\
      vertical-align: middle; font-size: 21px; height: 20px; color: #D4D0C8; cursor: default;\
    }\
    DIV.MzTreeView DIV SPAN.pm\
    {\
      width: "+ (this.navigator=="msie"||this.navigator=="opera" ? "11" : "9") +"px;\
      height: "+ (this.navigator=="netscape"?"9":(this.navigator=="firefox"?"10":"11")) +"px;\
      font-size: 7pt;\
      overflow: hidden;\
      margin-left: -16px;\
      margin-right: 5px;\
      color: #000080; \
      vertical-align: middle;\
      border: 1px solid #D4D0C8;\
      cursor: "+ (this.navigator=="msie" ? "hand" : "pointer") +";\
      padding: 0 2px 0 2px;\
      text-align: center;\
      background-color: #F0F0F0;\
    }";
  }
  style += "<\/style>";
  document.write(style);
};

//当根节点为空的时候做的处理
MzTreeView.prototype.atRootIsEmpty = function()
{
  var RCN = this.node["0"].childNodes;
  for(var i=0; i<RCN.length; i++)
  {
    if(!RCN[i].isLoad) this.expand(RCN[i].id);
    if (RCN[i].text=="")
    {
      var node = RCN[i].childNodes[0], HCN  = node.hasChild;
      //if(this.wordLine)
		  if(false)
      {
        var span = this.getElementById(this.name +"_tree_"+ node.id);
        span = span.childNodes[0].childNodes[0].childNodes[0];
        span.innerHTML = RCN[i].childNodes.length>1 ? "┌" : "─";
      }
      else
      {
        node.iconExpand  =  RCN[i].childNodes.length>1 ? HCN ? "PM0" : "L0" : HCN ? "PM3" : "L3"
        this.getElementById(this.name +"_expand_"+ node.id).src = this.icons[node.iconExpand].src;
      }
    }
  }
};

//初始化数据源里的数据以便后面的快速检索
MzTreeView.prototype.dataFormat = function()
{
  var a = new Array();
  for (var id in this.nodes) a[a.length] = id;
  this.names = a.join(this._d + this._d);
  this.totalNode = a.length; a = null;
};

//在数据源检索所需的数据节点
//id  客户端节点对应的id
MzTreeView.prototype.load = function(id)
{
  var node = this.node[id], d = this.divider, _d = this._d;
  var sid = node.sourceIndex.substr(node.sourceIndex.indexOf(d) + d.length);
  var reg = new RegExp("(^|"+_d+")"+ sid +d+"[^"+_d+d +"]+("+_d+"|$)", "g");
  var cns = this.names.match(reg), tcn = this.node[id].childNodes; if (cns){
  reg = new RegExp(_d, "g"); for (var i=0; i<cns.length; i++)
  tcn[tcn.length] = this.nodeInit(cns[i].replace(reg, ""), id); }
  node.isLoad = true;
};

//初始化节点信息, 根据 this.nodes 数据源生成节点的详细信息
//sourceIndex 数据源中的父子节点组合的字符串 0_1
//parentId    当前树节点在客户端的父节点的 id
MzTreeView.prototype.nodeInit = function(sourceIndex, parentId)
{
  this.index++;
  var source= this.nodes[sourceIndex], d = this.divider;
  var text  = this.getAttribute(source, "text");
  var hint  = this.getAttribute(source, "hint");
  var sid   = sourceIndex.substr(sourceIndex.indexOf(d) + d.length);
  this.node[this.index] =
  {
    "id"    : this.index,
    "text"  : text,
    "hint"  : hint ? hint : text,
    "icon"  : this.getAttribute(source, "icon"),
    "path"  : this.node[parentId].path + d + this.index,
    "isLoad": false,
    "isExpand": false,
    "parentId": parentId,
    "parentNode": this.node[parentId],
    "sourceIndex" : sourceIndex,
    "childAppend" : ""
  };
     this.nodes[sourceIndex] = "index:"+ this.index +";"+ source;
     this.node[this.index].hasChild = this.names.indexOf(this._d + sid + d)>-1;
  if(this.node[this.index].hasChild)  this.node[this.index].childNodes = [];
  return this.node[this.index];
};

//从XML格式字符串里提取信息
//source 数据源里的节点信息字符串(以后可以扩展对XML的支持)
//name   要提取的属性名
MzTreeView.prototype.getAttribute = function(source, name)
{
  var reg = new RegExp("(^|;|\\s)"+ name +"\\s*:\\s*([^;]*)(\\s|;|$)", "i");
  if (reg.test(source)) return RegExp.$2.replace(/[\x0f]/g, ";"); return "";
};

//根据节点的详细信息生成HTML
//node   树在客户端的节点对象
//AtEnd  布尔值  当前要转换的这个节点是否为父节点的子节点集中的最后一项
MzTreeView.prototype.nodeToHTML = function(node, AtEnd)
{
  var source = this.nodes[node.sourceIndex];
  var target = this.getAttribute(source, "target");
  var data = this.getAttribute(source, "data");
  var url  = this.getAttribute(source, "url");
  if(!url) url = this.url;
  if(data) url += (url.indexOf("?")==-1?"?":"&") + data;
  if(!target) target = this.target;

  var id   = node.id;
  var HCN  = node.hasChild, isRoot = node.parentId=="0";
  if(isRoot && node.icon=="") node.icon = "root";
  if(node.icon=="" || typeof(this.icons[node.icon])=="undefined")
    node.icon = HCN ? "folder" : "file";
  node.iconExpand  = AtEnd ? "└" : "├";

  var HTML = "<DIV noWrap='True'><NOBR>";
  if(!isRoot)
  {
    node.childAppend = node.parentNode.childAppend + (AtEnd ? "　" : "│");
   // if(this.wordLine)
    if(false)
    {
      HTML += "<SPAN>"+ node.parentNode.childAppend + (AtEnd ? "└" : "├") +"</SPAN>";
      if(HCN) HTML += "<SPAN class='pm' id='"+ this.name +"_expand_"+ id +"'>+</SPAN>";
    }
    else
    {
      node.iconExpand  = HCN ? AtEnd ? "PM2" : "PM1" : AtEnd ? "L2" : "L1";
      HTML += "<SPAN>"+ this.word2image(node.parentNode.childAppend) +"<IMG "+
        "align='absmiddle' id='"+ this.name +"_expand_"+ id +"' "+
        "src='"+ this.icons[node.iconExpand].src +"' style='cursor: "+ (!node.hasChild ? "":
        (this.navigator=="msie"||this.navigator=="opera"? "hand" : "pointer")) +"'></SPAN>";
    }
  }
  HTML += "<IMG "+
    "align='absMiddle' "+
    "id='"+ this.name +"_icon_"+ id +"' "+
    "src='"+ this.icons[node.icon].src +"'><A "+
    "class='MzTreeview' hideFocus "+
    "id='"+ this.name +"_link_"+ id +"' "+
    "href='"+ url +"' "+
    "target='"+ target +"' "+
    "title='"+ node.hint +"' "+
    "onfocus=\""+ this.name +".focusLink('"+ id +"')\" "+
    "onclick=\"return "+ this.name +".nodeClick('"+ id +"')\">"+ node.text +
  "</A></NOBR></DIV>";
  if(isRoot && node.text=="") HTML = "";

  HTML = "\r\n<SPAN id='"+ this.name +"_tree_"+ id +"'>"+ HTML 
  HTML +="<SPAN style='DISPLAY: none'></SPAN></SPAN>";
  return HTML;
};

//在使用图片的时候对 node.childAppend 的转换
MzTreeView.prototype.word2image = function(word)
{
  var str = "";
  for(var i=0; i<word.length; i++)
  {
    var img = "";
    switch (word.charAt(i))
    {
      case "│" : img = "L4"; break;
      case "└" : img = "L2"; break;
      case "　" : img = "empty"; break;
      case "├" : img = "L1"; break;
      case "─" : img = "L3"; break;
      case "┌" : img = "L0"; break;
    }
    if(img!="")
      str += "<IMG align='absMiddle' src='"+ this.icons[img].src +"' height='20'>";
  }
  return str;
}


//将某个节点下的所有子节点转化成详细的<HTML>元素表达
//id 树的客户端节点 id
MzTreeView.prototype.buildNode = function(id)
{
  if(this.node[id].hasChild)
  {
    var tcn = this.node[id].childNodes, str = "";
    for (var i=0; i<tcn.length; i++)
      str += this.nodeToHTML(tcn[i], i==tcn.length-1);
    var temp = this.getElementById(this.name +"_tree_"+ id).childNodes;
    temp[temp.length-1].innerHTML = str;
  }
};

//聚集到客户端生成的某个节点上
//id  客户端树节点的id
MzTreeView.prototype.focusClientNode      = function(id)
{
  if(!this.currentNode) this.currentNode=this.node["0"];

  var a = this.getElementById(this.name +"_link_"+ id); if(a){ a.focus();
  var link = this.getElementById(this.name +"_link_"+ this.currentNode.id);
  if(link)with(link.style){color="";   backgroundColor="";}
  with(a.style){color = this.colors.highLightText;
  backgroundColor = this.colors.highLight;}
  this.currentNode= this.node[id];}
};

//焦点聚集到树里的节点链接时的处理
//id 客户端节点 id
MzTreeView.prototype.focusLink= function(id)
{
  if(this.currentNode && this.currentNode.id==id) return;
  this.focusClientNode(id);
};

//点击展开树节点的对应方法
MzTreeView.prototype.expand   = function(id, sureExpand)
{
  var node  = this.node[id];
  if (sureExpand && node.isExpand) return;
  if (!node.hasChild) return;
  var area  = this.getElementById(this.name +"_tree_"+ id);
  if (area)   area = area.childNodes[area.childNodes.length-1];
  if (area)
  {
    var icon  = this.icons[node.icon];
    var iconE = this.iconsExpand[node.icon];
    var Bool  = node.isExpand = sureExpand || area.style.display == "none";
    var img   = this.getElementById(this.name +"_icon_"+ id);
    if (img)  img.src = !Bool ? icon.src :typeof(iconE)=="undefined" ? icon.src : iconE.src;
    var exp   = this.icons[node.iconExpand];
    var expE  = this.iconsExpand[node.iconExpand];
    var expand= this.getElementById(this.name +"_expand_"+ id);
    if (expand)
    {
      //if(this.wordLine) 
	  if(false)
		  expand.innerHTML = !Bool ? "+"  : "-";
      else expand.src = !Bool ? exp.src : typeof(expE) =="undefined" ? exp.src  : expE.src;
    }
    if(!Bool && this.currentNode.path.indexOf(node.path)==0 && this.currentNode.id!=id)
    {
      try{this.getElementById(this.name +"_link_"+ id).click();}
      catch(e){this.focusClientNode(id);}
    }
    area.style.display = !Bool ? "none" : "block";//(this.navigator=="netscape" ? "block" : "");
    if(!node.isLoad)
    {
      this.load(id);
      if(node.id=="0") return;

      //当子节点过多时, 给用户一个正在加载的提示语句
      if(node.hasChild && node.childNodes.length>200)
      {
        setTimeout(this.name +".buildNode('"+ id +"')", 1);
        var temp = this.getElementById(this.name +"_tree_"+ id).childNodes;
        temp[temp.length-1].innerHTML = "<DIV noWrap><NOBR><SPAN>"+ (this.wordLine ?
        node.childAppend +"└" : this.word2image(node.childAppend +"└")) +"</SPAN>"+
        "<IMG border='0' height='16' align='absmiddle' src='"+this.icons["file"].src+"'>"+
        "<A style='background-Color: "+ this.colors.highLight +"; color: "+
        this.colors.highLightText +"; font-size: 9pt'>请稍候...</A></NOBR></DIV>";
      }
      else this.buildNode(id);
    }
  }
};

//节点链接单击事件处理方法
//id 客户端树节点的 id
MzTreeView.prototype.nodeClick = function(id)
{
  var source = this.nodes[this.node[id].sourceIndex];
  eval(this.getAttribute(source, "method"));
  return !(!this.getAttribute(source, "url") && this.url=="#");
};

//为配合系统初始聚集某节点而写的函数, 得到某节点在数据源里的路径
//sourceId 数据源里的节点 id
MzTreeView.prototype.getPath= function(sourceId)
{
  
Array.prototype.indexOf = function(item)
  {
    for(var i=0; i<this.length; i++)
    {
      if(this[i]==item) return i;
    }
    return -1;
  };
  var _d = this._d, d = this.divider;
  var A = new Array(), id=sourceId; A[0] = id;
  while(id!="0" && id!="")
  {
    var str = "(^|"+_d+")([^"+_d+d+"]+"+d+ id +")("+_d+"|$)";
    if (new RegExp(str).test(this.names))
    {
      id = RegExp.$2.substring(0, RegExp.$2.indexOf(d));
      if(A.indexOf(id)>-1) break;
      A[A.length] = id;
    }
    else break;
  }
  return A.reverse();
};

//在源代码里指定 MzTreeView 初始聚集到某个节点
//sourceId 节点在数据源里的 id
MzTreeView.prototype.focus = function(sourceId, defer)
{
  if (!defer)
  {
    setTimeout(this.name +".focus('"+ sourceId +"', true)", 100);
    return;
  }
  var path = this.getPath(sourceId);
  if(path[0]!="0")
  {
    alert("节点 "+ sourceId +" 没有正确的挂靠有效树节点上！\r\n"+
      "节点 id 序列 = "+ path.join(this.divider));
    return;
  }
  var root = this.node["0"], len = path.length;
  for(var i=1; i<len; i++)
  {
    if(root.hasChild)
    {
      var sourceIndex= path[i-1] + this.divider + path[i];
      for (var k=0; k<root.childNodes.length; k++)
      {
        if (root.childNodes[k].sourceIndex == sourceIndex)
        {
          root = root.childNodes[k];
          if(i<len - 1) this.expand(root.id, true);
          else this.focusClientNode(root.id);
          break;
        }
      }
    }
  }
};

//树的单击事件处理函数
MzTreeView.prototype.clickHandle = function(e)
{
  e = window.event || e; e = e.srcElement || e.target;
  //alert(e.tagName)
  switch(e.tagName)
  {
    case "IMG" :
      if(e.id)
      {
        if(e.id.indexOf(this.name +"_icon_")==0)
          this.focusClientNode(e.id.substr(e.id.lastIndexOf("_") + 1));
        else if (e.id.indexOf(this.name +"_expand_")==0)
          this.expand(e.id.substr(e.id.lastIndexOf("_") + 1));
      }
      break;
    case "A" :
      if(e.id) this.focusClientNode(e.id.substr(e.id.lastIndexOf("_") + 1));
      break;
    case "SPAN" :
      if(e.className=="pm")
        this.expand(e.id.substr(e.id.lastIndexOf("_") + 1));
      break;
    default :
      if(this.navigator=="netscape") e = e.parentNode;
      if(e.tagName=="SPAN" && e.className=="pm")
        this.expand(e.id.substr(e.id.lastIndexOf("_") + 1));
      break;
  }
};

//MzTreeView 双击事件的处理函数
MzTreeView.prototype.dblClickHandle = function(e)
{
  e = window.event || e; e = e.srcElement || e.target;
  if((e.tagName=="A" || e.tagName=="IMG")&& e.id)
  {
    var id = e.id.substr(e.id.lastIndexOf("_") + 1);
    if(this.node[id].hasChild) this.expand(id);
  }
};

//回到树当前节点的父层节点
MzTreeView.prototype.upperNode = function()
{
  if(!this.currentNode) return;
  if(this.currentNode.id=="0" || this.currentNode.parentId=="0") return;
  if (this.currentNode.hasChild && this.currentNode.isExpand)
    this.expand(this.currentNode.id, false);
  else this.focusClientNode(this.currentNode.parentId);
};

//展开当前节点并
MzTreeView.prototype.lowerNode = function()
{
  if (!this.currentNode) this.currentNode = this.node["0"];
  if (this.currentNode.hasChild)
  {
    if (this.currentNode.isExpand)
      this.focusClientNode(this.currentNode.childNodes[0].id);
    else this.expand(this.currentNode.id, true);
  }
}

//聚集到树当前节点的上一节点
MzTreeView.prototype.pervNode = function()
{
  if(!this.currentNode) return; var e = this.currentNode;
  if(e.id=="0") return; var a = this.node[e.parentId].childNodes;
  for(var i=0; i<a.length; i++){if(a[i].id==e.id){if(i>0){e=a[i-1];
  while(e.hasChild){this.expand(e.id, true);
  e = e.childNodes[e.childNodes.length - 1];}
  this.focusClientNode(e.id); return;} else {
  this.focusClientNode(e.parentId); return;}}}
};

//聚集到树当前节点的下一节点
MzTreeView.prototype.nextNode = function()
{
  var e = this.currentNode; if(!e) e = this.node["0"];
  if (e.hasChild){this.expand(e.id, true);
  this.focusClientNode(e.childNodes[0].id); return;}
  while(typeof(e.parentId)!="undefined"){
  var a = this.node[e.parentId].childNodes;
  for(var i=0; i<a.length; i++){ if(a[i].id==e.id){
  if(i<a.length-1){this.focusClientNode(a[i+1].id); return;}
  else e = this.node[e.parentId];}}}
};

//展开树的所有节点
MzTreeView.prototype.expandAll = function()
{
  if(this.totalNode>500) if(
    confirm("您是否要停止展开全部节点？\r\n\r\n节点过多！展开很耗时")) return;
  if(this.node["0"].childNodes.length==0) return;
  var e = this.node["0"].childNodes[0];
  var isdo = t = false;
  while(e.id != "0")
  {
    var p = this.node[e.parentId].childNodes, pn = p.length;
    if(p[pn-1].id==e.id && (isdo || !e.hasChild)){e=this.node[e.parentId]; isdo = true;}
    else
    {
      if(e.hasChild && !isdo)
      {
        this.expand(e.id, true), t = false;
        for(var i=0; i<e.childNodes.length; i++)
        {
          if(e.childNodes[i].hasChild){e = e.childNodes[i]; t = true; break;}
        }
        if(!t) isdo = true;
      }
      else
      {
        isdo = false;
        for(var i=0; i<pn; i++)
        {
          if(p[i].id==e.id) {e = p[i+1]; break;}
        }
      }
    }
  }
};

//本树将要用动的图片的字义及预载函数
//path 图片存放的路径名
MzTreeView.prototype.setIconPath  = function(path)
{
  var k = 0, d = new Date().getTime();
  for(var i in this.icons)
  {
    var tmp = this.icons[i];
    this.icons[i] = new Image();
    this.icons[i].src = path + tmp;
    if(k==9 && (new Date().getTime()-d)>20)
      this.wordLine = true; k++;
  }
  for(var i in this.iconsExpand)
  {
    var tmp = this.iconsExpand[i];
    this.iconsExpand[i]=new Image();
    this.iconsExpand[i].src = path + tmp;
  }
};

//设置树的默认链接
//url 默认链接  若不设置, 其初始值为 #
MzTreeView.prototype.setURL     = function(url){this.url = url;};

//设置树的默认的目标框架名 target
//target 目标框架名  若不设置, 其初始值为 _self
MzTreeView.prototype.setTarget  = function(target){this.target = target;};
// -->