function Browser()
{
	var b = navigator.appName;
	this.name = b;
	this.version = navigator.appVersion;
	this.versionNr = parseInt(this.version);
}
Browser.prototype.dump = function()
{
	var t = "BROWSER ";
	t += "name = " + this.name + "; ";
	t += "version = " + this.version + "; ";
	t += "versionNr = " + this.versionNr + "; ";
	return t;
};

BROWSER=new Browser();

function Dhtml()
{
}

Dhtml.prototype.getElementByDocumentId = function(doc, id)
{
	return doc.getElementById(id);
};

Dhtml.prototype.getElementById = function(id)
{
    return this.getElementByDocumentId(document, id);
};

Dhtml.prototype.getHtmlContent = function(elem)
{
	return elem.innerHTML;
};

Dhtml.prototype.setHtmlContent = function(elem, text)
{
	elem.innerHTML = text;
};

/* -------------- ONE HANDLER SOLUTION ------------------
The simple solution, that works on msie, ns4, ns6, opera, when only
one handler per object is used.
*/
Dhtml.prototype.setObjectEventHandler = function(object, eventType, handler)
{
	object["on" + eventType] = handler;
};
Dhtml.prototype.setDocumentEventHandler = function(eventType, handler)
{
    eval("document.on" + eventType + " = handler");
};
Dhtml.prototype.getObjectEventHandler = function(object,eventType, handler)
{
	var ret = object["on" + eventType];
	return ret == null || ret + "" == "undefined" ? null : ret;
};
Dhtml.prototype.getDocumentEventHandler = function(eventType, handler)
{
    var ret = document["on" + eventType];
	return ret == null || ret + "" == "undefined" ? null : ret;
};

/* -------------- MULTIPLE HANDLER SOLUTION ------------------
The sophisticated solution, that works on msie5 and ns6, when
multiple handlers per object are used.
*/
Dhtml.prototype.setObjectEventHandlerM = function(object, eventType, handler)
{
	if (object.addEventListener) // DOM
		object.addEventListener(eventType, handler, false);
	else if (object.attachEvent) // MSIE < 9
		object.attachEvent("on" + eventType, handler);
	else // Fallback
		object["on" + eventType] = handler;
};
Dhtml.prototype.setDocumentEventHandlerM = function(eventType, handler)
{
	if (object.addEventListener) // DOM
		document.getElementsByTagName("body").item(0).addEventListener(eventType, handler, true);
	else if (object.attachEvent) // MSIE < 9
		document.getElementsByTagName("body").item(0).attachEvent("on" + eventType, handler);
	else // Fallback
		document["on" + eventType] = handler;
};

Dhtml.prototype.getObjectKey = function(object)
{
	if (object == null)
		return null;
	if (typeof object == "object")
	{
		if (object.id + "" != "undefined")
			return object.id;
		else
			return object.name;
	}
};

Dhtml.prototype.getWindowWidth = function()
{
	var ret = -1;
	if (window.innerWidth + "" != "undefined")
	{
		ret = window.innerWidth;
	}
	else if (document.documentElement && (document.documentElement.clientWidth + "" != "undefined"))
	{
		ret = document.documentElement.clientWidth;
	}
	else if (document.body && (document.body.clientWidth + "" != "undefined"))
	{
		ret = document.body.clientWidth;
	}
	if (ret > 0)
	{
		return ret;
	}
	else
	{
		return screen.availWidth;
	}
};

Dhtml.prototype.getWindowHeight = function()
{
	var ret = -1;
	if (window.innerHeight + "" != "undefined")
	{
		ret = window.innerHeight;
	}
	else if (document.documentElement && (document.documentElement.clientHeight + "" != "undefined"))
	{
		ret = document.documentElement.clientHeight;
	}
	else if (document.body && (document.body.clientHeight + "" != "undefined"))
	{
		ret = document.body.clientHeight;
	}
	if (ret > 0)
	{
		return ret;
	}
	else
	{
		return screen.availHeight;
	}
};

// Firefox 18 / Windows    : W: x,   x+5, x, x, x    H: x,   x+5, x, x, x+5
// Chrome 24 / Windows     : W: x+6, x+6, x, x, x
// MSIE 9 / Windows        : W: x+6, x+6, x, x, x

Dhtml.prototype.getDocumentWidth = function()
{
    return Math.max(
        document.body["scrollWidth"],
        document.documentElement[ "scrollWidth" ],
        document.body[ "offsetWidth" ],
        document.documentElement[ "offsetWidth" ],
        document.documentElement[ "clientWidth" ]
    ) - 2;
};

Dhtml.prototype.getDocumentHeight = function()
{
    return Math.max(
        document.body[ "scrollHeight" ],
        document.documentElement[ "scrollHeight" ],
        document.body[ "offsetHeight" ],
        document.documentElement[ "offsetHeight" ],
        document.documentElement[ "clientHeight" ]
    ) - 2;
};

DHTML = new Dhtml();

/**
Usage:
- Define global variable R = new Rollover() in window JS code
- in onload call R.loadIcons("i0", "i1", "i2", ...);
- in img tag: id="x_menu_<index>" onMouseOut="R.restoreImage(<index>);" onMouseOver="R.swapImage(<index>);"
  where <index> is the array index, e.g. 1 for image i1.
**/
function Rollover()
{
	this.menuIcons = new Array();
	this.roIcons = new Array();
}
Rollover.prototype.loadIcons = function()
{
	for (var i = 0; i < Rollover.prototype.loadIcons.arguments.length; i++)
		this.loadMenuIcon(Rollover.prototype.loadIcons.arguments[i],i);
};
Rollover.prototype.getImage = function(index)
{
	return DHTML.getElementById("x_menu_" + index);
};
Rollover.prototype.swapImage = function(index)
{
	var image = this.getImage(index);
	if (image)
	{
		var other = this.roIcons[index];
		if (other) image.src = other.src;
	}
};
Rollover.prototype.restoreImage = function(index)
{
	var image = this.getImage(index);
	if (image)
	{
		var other = this.menuIcons[index];
		if (other) image.src = other.src;
	}
};
Rollover.prototype.loadMenuIcon = function(src, index)
{
	var image = new Image();
	image.src = src;
	this.menuIcons[index] = image;
	image = new Image();
	image.src = src.substring(0, src.length-3) + "ro.gif";
	this.roIcons[index] = image;
};

