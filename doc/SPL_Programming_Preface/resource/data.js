var tree = new MzTreeView("tree");
tree.setIconPath("images/");
 tree.nodes["0_1"] = "text:";
 tree.nodes["1_3"] = "text:Preface;url:topics/1.html";
 tree.nodes["1_4"] = "text:Table of contents;url:topics/2.html";
 tree.nodes["1_5"] = "text:Download PDF;url:topics/3.html";
tree.setTarget("mainFrame");
document.write(tree.toString());