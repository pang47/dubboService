
c_ajax = function(_o){
	var _r;
	if(window.XMLHttpRequest)
		_r = new XMLHttpRequest();
	if(window.ActiveXObject)
		_r = new ActiveXObject('Microsoft.XMLHTTP'); // for IE5,IE6
	if(!_r){
		alert("抱歉,你的浏览器不支持,请升级浏览器,或换一个浏览器继续使用");
		return;
	}
	var _hfl = {};
	var _sf = function(_d){};
	if(_o.success)
		_sf = _o.success;
	var _ef = function(_d,_m){};
	if(_o.error)
		_ef = _o.error;
	_r.onreadystatechange = function(){
		//readyState from 0 to 4;
		//0:初始化成功,还没调用open
		//1:正在发送,调用open&send
		//2:服务端数据接受完成，send执行完成
		//3:正在解析
		//4:解析完成
		//status is http status;
		if(_r.readyState == 4){
			try{
				var _d = JSON.stringify(_r.responseText);
			}catch (e) {
				alert('please check your answer data again.');
			}
			switch (_r.status) {
			case 200://成功
				_sf(_d);
				break;
			case 401:
				_ef(_d, 'error request.');
				break;
			case 404:
				_ef(_d, 'source not found');
				break;
			case 403:
				_ef(_d, 'request fobbiden');
				break;
			case 401:
				_ef(_d, 'request unauth');
				break;
			default:
				_ef(_d, '');
				break;
			}
			
		}
	}
	//处理json数据
	var _sd = '';
	if(_o.data&&_o.dataType&&_o.dataType=='json'){
		for(var _i in _o.data){
			_sd += (_sd?'&':'')+ _i +'=' + JSON.stringify(_o.data[_i]);
		}
	}
	console.log(_sd);
	_r.open(_o.type?_o.type:'GET',_o.url,_o.async?_o.async:true);
	_r.setRequestHeader("Content-Type", "application/x-www-form-urlencoded;");
	_r.send(_sd);
}

function isJson(str){
	if(typeof str == 'string'){
		try{
			var _o = JSON.parse(str);
			if(_o && typeof _o == 'object')
				return true;
			else
				return false;
		}catch (e) {
			return false;
		}
	}
}

;(function($){
	$.fn.extend({
		'formatJson':function(){
			var _v = this.val();
			if(!_v) return;
			try{
				this.val(JSON.stringify(JSON.parse(_v), null, 4));
			}catch(e){
				alert(e.description);
			}
			return this;
		}
	});
})(jQuery);


