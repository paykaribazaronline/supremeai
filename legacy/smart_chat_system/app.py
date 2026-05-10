
from flask import Flask, render_template, request, jsonify, session, send_from_directory
from flask_socketio import SocketIO, emit
from smart_chat_system import SmartChatSystem
from plan_analyzer import PlanAnalyzer
from image_processor import ImageProcessor
import os
import uuid
import psutil
import time
from datetime import datetime

# Load configuration
def load_config():
    config_path = os.path.join(os.path.dirname(__file__), 'config.json')
    if os.path.exists(config_path):
        with open(config_path, 'r') as f:
            return json.load(f)
    return {
        "server": {
            "host": "localhost",
            "port": 5000,
            "debug": False
        }
    }

config = load_config()

app = Flask(__name__)
app.config['SECRET_KEY'] = os.environ.get('SECRET_KEY', 'dev-secret-key-change-in-production')
app.config['MAX_CONTENT_LENGTH'] = 16 * 1024 * 1024  # 16MB max upload size
socketio = SocketIO(app, cors_allowed_origins="*")

# স্মার্ট চ্যাট সিস্টেম ইনিশিয়ালাইজ করা
chat_system = SmartChatSystem()

# প্ল্যান অ্যানালাইজার ইনিশিয়ালাইজ করা
plan_analyzer = PlanAnalyzer()

# ইমেজ প্রসেসর ইনিশিয়ালাইজ করা
current_dir = os.path.dirname(os.path.abspath(__file__))
uploads_dir = os.path.join(current_dir, "uploads")
image_processor = ImageProcessor(uploads_dir)

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

@app.route('/api/plan/analyze', methods=['POST'])
def analyze_plan():
    """নতুন প্ল্যানের সাথে বিদ্যমান প্ল্যানের সামঞ্জস্যতা বিশ্লেষণ করে"""
    data = request.json

    # ইউজার আইডি পাওয়া
    if 'user_id' not in session:
        return jsonify({"success": False, "message": "ইউজার আইডি পাওয়া যায়নি"}), 400

    user_id = session['user_id']
    new_plan = data.get('plan', '')

    if not new_plan:
        return jsonify({"success": False, "message": "প্ল্যান প্রদান করতে হবে"}), 400

    # বিদ্যমান প্ল্যান লোড করা
    existing_plans = chat_system.get_plans(active_only=True)

    # প্ল্যান অ্যানালিসিস করা
    compatibility_report = plan_analyzer.analyze_plan_compatibility(new_plan, existing_plans)

    # ফিউচার স্টেট প্রেডিক্ট করা
    future_state = plan_analyzer.predict_future_state(new_plan, existing_plans)

    return jsonify({
        "success": True,
        "compatibility_report": compatibility_report,
        "future_state": future_state
    })

@app.route('/api/image/upload', methods=['POST'])
def upload_image():
    """ইমেজ আপলোড করে"""
    # ইউজার আইডি পাওয়া
    if 'user_id' not in session:
        return jsonify({"success": False, "message": "ইউজার আইডি পাওয়া যায়নি"}), 400

    user_id = session['user_id']

    # চেক করা যে ফাইল আপলোড করা হয়েছে কিনা
    if 'image' not in request.files:
        return jsonify({"success": False, "message": "কোন ইমেজ ফাইল পাওয়া যায়নি"}), 400

    file = request.files['image']

    # ফাইল নাম চেক করা
    if file.filename == '':
        return jsonify({"success": False, "message": "কোন ফাইল নির্বাচন করা হয়নি"}), 400

    # ইমেজ প্রসেস করা
    try:
        # ফাইল থেকে বাইনারি ডাটা পড়া
        image_data = file.read()

        # বাইনারি ডাটা থেকে Base64 তৈরি করা
        import base64
        import mimetypes
        mime_type, _ = mimetypes.guess_type(file.filename)
        if not mime_type or not mime_type.startswith('image/'):
            return jsonify({"success": False, "message": "অবৈধ ইমেজ ফাইল"}), 400

        base64_data = f"data:{mime_type};base64,{base64.b64encode(image_data).decode('utf-8')}"

        # ইমেজ প্রসেস করা
        result = image_processor.process_base64_image(base64_data, user_id)

        if result.get('success'):
            return jsonify(result)
        else:
            return jsonify(result), 400
    except Exception as e:
        return jsonify({"success": False, "message": f"ইমেজ আপলোডে সমস্যা: {str(e)}"}), 500

@app.route('/api/image/upload-base64', methods=['POST'])
def upload_image_base64():
    """Base64 এনকোডেড ইমেজ আপলোড করে"""
    # ইউজার আইডি পাওয়া
    if 'user_id' not in session:
        return jsonify({"success": False, "message": "ইউজার আইডি পাওয়া যায়নি"}), 400

    user_id = session['user_id']
    data = request.json

    # Base64 ডাটা পাওয়া
    base64_data = data.get('image', '')

    if not base64_data:
        return jsonify({"success": False, "message": "ইমেজ ডাটা প্রদান করতে হবে"}), 400

    # ইমেজ প্রসেস করা
    result = image_processor.process_base64_image(base64_data, user_id)

    if result.get('success'):
        return jsonify(result)
    else:
        return jsonify(result), 400

@app.route('/uploads/<path:filename>')
def uploaded_file(filename):
    """আপলোড করা ফাইল সার্ভ করে"""
    return send_from_directory(uploads_dir, filename)

@app.route('/api/status')
def api_status():
    """API status endpoint for monitoring"""
    try:
        return jsonify({
            'status': 'running',
            'timestamp': datetime.utcnow().isoformat(),
            'services': {
                'chat_system': 'running' if chat_system else 'stopped',
                'plan_analyzer': 'running' if plan_analyzer else 'stopped',
                'image_processor': 'running' if image_processor else 'stopped'
            },
            'version': '1.0.0'
        })
    except Exception as e:
        return jsonify({
            'status': 'error',
            'error': str(e),
            'timestamp': datetime.utcnow().isoformat()
        }), 500

@app.route('/health')
def health_check():
    """System health check endpoint for monitoring"""
    try:
        # Get CPU usage
        cpu_percent = psutil.cpu_percent(interval=1)

        # Get memory information
        memory = psutil.virtual_memory()
        memory_used = round(memory.used / (1024**3), 2)  # Convert to GB
        memory_total = round(memory.total / (1024**3), 2)  # Convert to GB
        memory_percent = memory.percent

        # Get disk information
        disk = psutil.disk_usage('/')
        disk_used = round(disk.used / (1024**3), 2)  # Convert to GB
        disk_total = round(disk.total / (1024**3), 2)  # Convert to GB
        disk_percent = disk.percent

        # Calculate uptime (in seconds since process start)
        uptime_seconds = time.time() - psutil.boot_time()
        uptime_hours = round(uptime_seconds / 3600, 2)

        # Get process information
        process = psutil.Process()
        process_memory = round(process.memory_info().rss / (1024**2), 2)  # Convert to MB

        # Check if essential services are running
        services_status = {
            'chat_system': 'running' if chat_system else 'stopped',
            'plan_analyzer': 'running' if plan_analyzer else 'stopped',
            'image_processor': 'running' if image_processor else 'stopped'
        }

        # Determine overall status
        overall_status = 'healthy'
        if cpu_percent > 80 or memory_percent > 90 or disk_percent > 90:
            overall_status = 'degraded'
        if cpu_percent > 95 or memory_percent > 95 or disk_percent > 95:
            overall_status = 'critical'

        return jsonify({
            'status': overall_status,
            'timestamp': datetime.utcnow().isoformat(),
            'uptime': f"{uptime_hours}h",
            'cpu': {
                'usage': cpu_percent,
                'cores': psutil.cpu_count()
            },
            'memory': {
                'usage': memory_used,
                'total': memory_total,
                'percent': memory_percent,
                'process_memory': f"{process_memory}MB"
            },
            'disk': {
                'usage': disk_used,
                'total': disk_total,
                'percent': disk_percent
            },
            'services': services_status
        })
    except Exception as e:
        return jsonify({
            'status': 'error',
            'error': str(e),
            'timestamp': datetime.utcnow().isoformat()
        }), 500

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
