#  NoNavBarNav
  

 - 隐藏了底部导航栏，通过从屏幕边缘往里滑来实现导航的功能
 - 目的是不让Amoled屏幕因为导航栏而出现烧屏
 
**参考并使用了下面项目的部分代码，thx！ :**
-	[ztc1997/HideableNavBar](https://github.com/ztc1997/HideableNavBar)
-	[GravityBox/GravityBox](https://github.com/GravityBox/GravityBox)

###*1.0 :*
	
   在屏幕底部边缘往上滑时，不同触摸位置对应的按键：
   \#define w 屏幕宽度（px , 短边）
   \#define h 屏幕高度（px , 长边）
   - 竖屏:
   |--无意义(1/5w)-->|--app switch(2/5w)-->|--home(1-1/3w)-->|--back(1-1/10w)-->|--menu(1w)-->|
   - 横屏:
  将竖屏的区域划分逆时针转90度.
  
  这么划分的原因: 现在用的手机是xt1085(XD), 上面的各条分界线大概会对应xt1085底部的几个点: 
|---下角红外传感器 -- 扬声器挡条左边缘 -- 扬声器档条右边缘 -- 右下角红外传感器---|

*分界线的位置不可手动调整*
*没有屏蔽触摸事件的传递, 当前activity也会收到同样的触摸事件*
*粗略实现了基本功能, 代码乱*
