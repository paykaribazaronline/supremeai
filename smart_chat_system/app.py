
from flask import Flask, render_template, request, jsonify, session
from flask_socketio import SocketIO, emit
from smart_chat_system import SmartChatSystem
import os
import uuid

app = Flask(__name__)
app.config['SECRET_KEY'] = os.environ.get('SECRET_KEY', 'dev-secret-key-change-in-production')
socketio = SocketIO(app, cors_allowed_origins="*")

# স্মার্ট চ্যাট সিস্টেম ইনিশিয়ালাইজ করা
chat_system = SmartChatSystem()

@app.route('/')
def index():
    """মূল পেজ"""
    return render_template('index.html')

@app.route('/admin')
def admin():
    """এডমিন প্যানেল"""
    return render_template('admin.html')

@app.route('/api/chat', methods=['POST'])
def process_chat():
    """চ্যাট মেসেজ প্রসেস করে"""
    data = request.json

    # ইউজার আইডি পাওয়া বা তৈরি করা
    if 'user_id' not in session:
        session['user_id'] = str(uuid.uuid4())

    user_id = session['user_id']
    message = data.get('message', '')
    is_admin = data.get('is_admin', False)

    # মেসেজ প্রসেস করা
    result = chat_system.process_message(user_id, message, is_admin)

    return jsonify(result)

@app.route('/api/confirm', methods=['POST'])
def confirm_item():
    """আইটেম কনফার্ম বা প্রত্যাখ্যান করে"""
    data = request.json

    # ইউজার আইডি পাওয়া
    if 'user_id' not in session:
        return jsonify({"success": False, "message": "ইউজার আইডি পাওয়া যায়নি"}), 400

    user_id = session['user_id']
    item_id = data.get('item_id', '')
    confirmed = data.get('confirmed', False)

    # আইটেম কনফার্ম করা
    result = chat_system.confirm_item(item_id, confirmed, user_id)

    return jsonify(result)

@app.route('/api/pending', methods=['GET'])
def get_pending_confirmations():
    """পেন্ডিং কনফার্মেশনের তালিকা পায়"""
    # ইউজার আইডি পাওয়া
    if 'user_id' not in session:
        return jsonify({"success": False, "message": "ইউজার আইডি পাওয়া যায়নি"}), 400

    user_id = session['user_id']
    pending_items = chat_system.get_pending_confirmations(user_id)

    return jsonify({"success": True, "items": pending_items})

@app.route('/api/rules', methods=['GET'])
def get_rules():
    """সকল রুলের তালিকা পায়"""
    active_only = request.args.get('active_only', 'true').lower() == 'true'
    rules = chat_system.get_rules(active_only)
    return jsonify({"success": True, "rules": rules})

@app.route('/api/plans', methods=['GET'])
def get_plans():
    """সকল প্ল্যানের তালিকা পায়"""
    active_only = request.args.get('active_only', 'true').lower() == 'true'
    plans = chat_system.get_plans(active_only)
    return jsonify({"success": True, "plans": plans})

@app.route('/api/commands', methods=['GET'])
def get_commands():
    """সকল কমান্ডের তালিকা পায়"""
    active_only = request.args.get('active_only', 'true').lower() == 'true'
    commands = chat_system.get_commands(active_only)
    return jsonify({"success": True, "commands": commands})

@app.route('/api/chat/history', methods=['GET'])
def get_chat_history():
    """চ্যাট হিস্ট্রি পায়"""
    # ইউজার আইডি পাওয়া
    if 'user_id' not in session:
        return jsonify({"success": False, "message": "ইউজার আইডি পাওয়া যায়নি"}), 400

    user_id = session['user_id']
    limit = request.args.get('limit', 100, type=int)
    chat_history = chat_system.get_chat_history(user_id, limit)

    return jsonify({"success": True, "chat_history": chat_history})

@app.route('/api/item', methods=['GET'])
def get_item():
    """আইডি দিয়ে একটি আইটেম পায়"""
    item_type = request.args.get('type', '')
    item_id = request.args.get('id', '')

    if not item_type or not item_id:
        return jsonify({"success": False, "message": "আইটেম টাইপ এবং আইডি প্রদান করতে হবে"}), 400

    item = chat_system.get_item_by_id(item_type, item_id)

    if item:
        return jsonify({"success": True, "item": item})
    else:
        return jsonify({"success": False, "message": "আইটেম পাওয়া যায়নি"}), 404

@app.route('/api/confirmations', methods=['GET'])
def get_confirmations():
    """কনফার্মেশন হিস্ট্রি পায়"""
    item_id = request.args.get('item_id', None)
    chat_id = request.args.get('chat_id', None)

    confirmations = chat_system.get_confirmation_history(item_id, chat_id)

    return jsonify({"success": True, "confirmations": confirmations})

# WebSocket ইভেন্ট হ্যান্ডলার
@socketio.on('connect')
def handle_connect():
    """ক্লায়েন্ট কানেক্ট হলে"""
    emit('connected', {'data': 'সার্ভারে সফলভাবে কানেক্ট হয়েছে'})

@socketio.on('disconnect')
def handle_disconnect():
    """ক্লায়েন্ট ডিসকানেক্ট হলে"""
    print('ক্লায়েন্ট ডিসকানেক্ট হয়েছে')

@socketio.on('chat_message')
def handle_chat_message(data):
    """চ্যাট মেসেজ হ্যান্ডল করে"""
    # ইউজার আইডি পাওয়া বা তৈরি করা
    if 'user_id' not in session:
        session['user_id'] = str(uuid.uuid4())

    user_id = session['user_id']
    message = data.get('message', '')
    is_admin = data.get('is_admin', False)

    # মেসেজ প্রসেস করা
    result = chat_system.process_message(user_id, message, is_admin)

    # ফলাফল ক্লায়েন্টে পাঠানো
    emit('chat_response', result)

@socketio.on('confirm_item')
def handle_confirm_item(data):
    """আইটেম কনফার্ম বা প্রত্যাখ্যান করে"""
    # ইউজার আইডি পাওয়া
    if 'user_id' not in session:
        emit('error', {'message': 'ইউজার আইডি পাওয়া যায়নি'})
        return

    user_id = session['user_id']
    item_id = data.get('item_id', '')
    confirmed = data.get('confirmed', False)

    # আইটেম কনফার্ম করা
    result = chat_system.confirm_item(item_id, confirmed, user_id)

    # ফলাফল ক্লায়েন্টে পাঠানো
    emit('confirmation_response', result)

@socketio.on('get_pending')
def handle_get_pending():
    """পেন্ডিং কনফার্মেশনের তালিকা পায়"""
    # ইউজার আইডি পাওয়া
    if 'user_id' not in session:
        emit('error', {'message': 'ইউজার আইডি পাওয়া যায়নি'})
        return

    user_id = session['user_id']
    pending_items = chat_system.get_pending_confirmations(user_id)

    # ফলাফল ক্লায়েন্টে পাঠানো
    emit('pending_items', {'items': pending_items})

if __name__ == '__main__':
    socketio.run(app, debug=True, host='0.0.0.0', port=5000)
