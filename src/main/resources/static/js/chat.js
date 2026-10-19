(function () {
    'use strict';

    const shell = document.querySelector('.chat-shell');
    if (!shell) return;

    const currentUser = shell.dataset.currentUser;
    const roomList = document.getElementById('roomList');
    const roomCount = document.getElementById('roomCount');
    const emptyState = document.getElementById('emptyState');
    const conversation = document.getElementById('conversation');
    const roomName = document.getElementById('roomName');
    const roomType = document.getElementById('roomType');
    const schoolName = document.getElementById('schoolName');
    const messageList = document.getElementById('messageList');
    const messageForm = document.getElementById('messageForm');
    const messageInput = document.getElementById('messageInput');
    const errorBox = document.getElementById('chatError');
    const connectionStatus = document.getElementById('connectionStatus');
    let rooms = [];
    let activeRoom = null;
    let socket = null;
    let reconnectTimer = null;
    let pollTimer = null;
    let pollInFlight = false;

    function setStatus(text, cssClass) {
        connectionStatus.textContent = text;
        connectionStatus.className = 'chat-status ' + cssClass;
    }

    function showError(text) {
        errorBox.textContent = text || '';
    }

    function messageUrl(roomId) {
        return '/api/chat/rooms/' + encodeURIComponent(roomId) + '/messages?size=50&page=0';
    }

    function stopPolling() {
        if (pollTimer) {
            window.clearInterval(pollTimer);
            pollTimer = null;
        }
    }

    function pollActiveRoom() {
        if (!activeRoom || pollInFlight) return;
        pollInFlight = true;
        loadMessages(activeRoom, false)
            .catch(function () { /* Retry on the next poll. */ })
            .finally(function () { pollInFlight = false; });
    }

    function startPolling() {
        if (pollTimer || !activeRoom) return;
        pollActiveRoom();
        pollTimer = window.setInterval(pollActiveRoom, 3000);
    }

    function scheduleReconnect() {
        if (reconnectTimer) return;
        reconnectTimer = window.setTimeout(function () {
            reconnectTimer = null;
            connectSocket();
        }, 2500);
    }

    function roleLabel(role) {
        return {
            ROLE_ADMIN: 'Quản trị hệ thống',
            ROLE_SCHOOL_ADMIN: 'Admin - Hiệu trưởng',
            ROLE_TEACHER: 'Giáo viên',
            ROLE_USER: 'Học sinh'
        }[role] || role || 'Thành viên';
    }

    function roomLabel(room) {
        return room.type === 'GLOBAL_ADMIN' ? 'Nhóm cấp hệ thống' : 'Nhóm giáo viên trong trường';
    }

    function renderRooms() {
        roomList.textContent = '';
        roomCount.textContent = String(rooms.length);
        if (!rooms.length) {
            const placeholder = document.createElement('div');
            placeholder.className = 'room-placeholder';
            placeholder.textContent = 'Bạn chưa có nhóm chat nào.';
            roomList.appendChild(placeholder);
            return;
        }
        rooms.forEach(function (room) {
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'room-button' + (activeRoom && activeRoom.id === room.id ? ' is-active' : '');
            button.dataset.roomId = String(room.id);
            const title = document.createElement('strong');
            title.textContent = room.name;
            const type = document.createElement('small');
            type.textContent = roomLabel(room);
            button.append(title, type);
            button.addEventListener('click', function () { selectRoom(room); });
            roomList.appendChild(button);
        });
    }

    function connectSocket() {
        if (socket && (socket.readyState === WebSocket.CONNECTING || socket.readyState === WebSocket.OPEN)) {
            return;
        }
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        try {
            socket = new WebSocket(protocol + '//' + window.location.host + '/ws');
        } catch (error) {
            setStatus('Đang dùng kết nối dự phòng', 'is-offline');
            startPolling();
            scheduleReconnect();
            return;
        }

        const currentSocket = socket;
        currentSocket.addEventListener('open', function () {
            setStatus('Đang hoạt động', 'is-online');
            stopPolling();
            if (activeRoom) subscribe(activeRoom.id);
        });
        currentSocket.addEventListener('close', function () {
            if (socket !== currentSocket) return;
            socket = null;
            setStatus('Đang dùng kết nối dự phòng', 'is-offline');
            startPolling();
            scheduleReconnect();
        });
        currentSocket.addEventListener('error', function () {
            if (socket === currentSocket) {
                setStatus('Đang dùng kết nối dự phòng', 'is-offline');
                startPolling();
            }
        });
        currentSocket.addEventListener('message', function (event) {
            try {
                const payload = JSON.parse(event.data);
                if (payload.type === 'message' && activeRoom && payload.roomId === activeRoom.id) {
                    appendMessage(payload.message);
                } else if (payload.type === 'error') {
                    showError(payload.message);
                }
            } catch (error) {
                showError('Không thể đọc dữ liệu chat.');
            }
        });
    }

    function subscribe(roomId) {
        if (socket && socket.readyState === WebSocket.OPEN) {
            socket.send(JSON.stringify({ type: 'subscribe', roomId: roomId }));
        }
    }

    function selectRoom(room) {
        activeRoom = room;
        renderRooms();
        emptyState.classList.add('is-hidden');
        conversation.classList.remove('is-hidden');
        roomName.textContent = room.name;
        roomType.textContent = roomLabel(room);
        schoolName.textContent = room.schoolName || '';
        messageList.textContent = '';
        showError('');
        loadMessages(room, true)
            .catch(function (error) { showError(error.message); });
        if (!socket || socket.readyState !== WebSocket.OPEN) {
            startPolling();
        }
    }

    function loadMessages(room, focusInput) {
        return fetch(messageUrl(room.id))
            .then(function (response) {
                if (!response.ok) throw new Error('Không thể tải lịch sử tin nhắn.');
                return response.json();
            })
            .then(function (data) {
                if (!activeRoom || activeRoom.id !== room.id) return;
                data.messages.forEach(appendMessage);
                messageList.scrollTop = messageList.scrollHeight;
                subscribe(room.id);
                if (focusInput) messageInput.focus();
            });
    }

    function appendMessage(message) {
        if (messageList.querySelector('[data-message-id="' + String(message.id) + '"]')) {
            return;
        }
        const row = document.createElement('div');
        row.className = 'message-row' + (message.senderId === currentUser ? ' is-mine' : '');
        row.dataset.messageId = String(message.id);
        const bubble = document.createElement('article');
        bubble.className = 'message-bubble';
        const meta = document.createElement('div');
        meta.className = 'message-meta';
        meta.textContent = message.senderName + ' · ' + roleLabel(message.senderRole);
        const text = document.createElement('div');
        text.className = 'message-text';
        text.textContent = message.content;
        const time = document.createElement('time');
        time.className = 'message-time';
        time.textContent = formatTime(message.createdAt);
        bubble.append(meta, text, time);
        row.appendChild(bubble);
        messageList.appendChild(row);
        messageList.scrollTop = messageList.scrollHeight;
    }

    function formatTime(value) {
        if (!value) return '';
        const date = new Date(value);
        return Number.isNaN(date.getTime()) ? value : date.toLocaleString('vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit' });
    }

    messageForm.addEventListener('submit', function (event) {
        event.preventDefault();
        const content = messageInput.value.trim();
        if (!activeRoom || !content) return;
        if (!socket || socket.readyState !== WebSocket.OPEN) {
            sendMessageOverHttp(content);
            return;
        }
        socket.send(JSON.stringify({ type: 'message', roomId: activeRoom.id, content: content }));
        messageInput.value = '';
        messageInput.focus();
    });

    function sendMessageOverHttp(content) {
        fetch('/api/chat/rooms/' + encodeURIComponent(activeRoom.id) + '/messages', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
            body: JSON.stringify({ content: content })
        })
            .then(function (response) {
                if (!response.ok) {
                    return response.text().then(function () {
                        throw new Error('Không thể gửi tin nhắn qua kết nối dự phòng.');
                    });
                }
                return response.json();
            })
            .then(function (message) {
                appendMessage(message);
                messageInput.value = '';
                messageInput.focus();
                showError('');
            })
            .catch(function (error) { showError(error.message); });
    }

    fetch('/api/chat/rooms')
        .then(function (response) {
            if (!response.ok) throw new Error('Không thể tải danh sách nhóm chat.');
            return response.json();
        })
        .then(function (data) {
            rooms = data;
            renderRooms();
            connectSocket();
            if (rooms.length) selectRoom(rooms[0]);
        })
        .catch(function (error) {
            roomList.textContent = '';
            showError(error.message);
            setStatus('Không tải được', 'is-offline');
        });
}());
