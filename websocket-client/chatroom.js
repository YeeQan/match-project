var socket = null;
var userId = null;

//强制关闭浏览器  调用websocket.close（）,进行正常关闭
window.onunload = function() {
	disconnect()
}

function connect(){
	userId = $("#userIdInput").val();
	var socketUrl="ws://127.0.0.1:5003/game/match/" + userId;
	socket = new WebSocket(socketUrl);
	//打开事件
	socket.onopen = function() {
		console.log("websocket 已打开 userId: " + userId);
	};
	//获得消息事件
	socket.onmessage = function(msg) {
		var serverMsg = "收到服务端信息: " + msg.data;
		console.log(serverMsg);
	};
	//关闭事件
	socket.onclose = function() {
		console.log("websocket 已关闭 userId: " + userId);
	};
	//发生了错误事件
	socket.onerror = function() {
		console.log("websocket 发生了错误 userId : " + userId);
	}
}

function disconnect(){
	socket.close();
}

// 用户加入
function addUser(){
	var chatMessage = {};
	var sender = userId;
	var type = "ADD_USER";
	chatMessage.sender = sender;
	chatMessage.type = type;
	console.log("用户:" + sender + "开始加入......");
	socket.send(JSON.stringify(chatMessage));
}

// 随机匹配
function matchUser(){
	var chatMessage = {};
	var sender = userId;
	var type = "MATCH_USER";
	chatMessage.sender = sender;
	chatMessage.type = type;
	console.log("用户:" + sender + "开始匹配......");
	socket.send(JSON.stringify(chatMessage));
}

// 取消匹配
function cancelMatch(){
	var chatMessage = {};
	var sender = userId;
	var type = "CANCEL_MATCH";
	chatMessage.sender = sender;
	chatMessage.type = type;
	console.log("用户:" + sender + "取消匹配......");
	socket.send(JSON.stringify(chatMessage));
}

// 游戏中
function userInPlay(){
	var chatMessage = {};
	var sender = userId;
	var data = $("#newScoreInput").val();
	var type = "PLAY_GAME";
	chatMessage.sender = sender;
	chatMessage.data = data;
	chatMessage.type = type;
	console.log("用户:" + sender + "更新分数为" + data);
	socket.send(JSON.stringify(chatMessage));
}

// 游戏结束
function gameover(){
	var chatMessage = {};
	var sender = userId;
	var type = "GAME_OVER";
	chatMessage.sender = sender;
	chatMessage.type = type;
	console.log("用户:" + sender + "结束游戏");
	socket.send(JSON.stringify(chatMessage));
}