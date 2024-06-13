#!/usr/local/bin/python
# coding: utf-8

import json
import subprocess
import threading
from datetime import datetime
from threading import Thread
import cv2
import mediapipe as mp
import time
import tensorflow as tf
import numpy as np
import os
import array
from flask import Flask, jsonify, request
import collections
import os
import throttle
from mediapipe.tasks import python
from mediapipe.tasks.python import vision
from pprint import pprint
import tflite_runtime.interpreter as tflite
from mediapipe.tasks.python import audio

# from fdlite import FaceDetection, FaceDetectionModel
# from fdlite.render import Colors, detections_to_render_data, render_to_image
import sqlite3
import pyaudio
import wave

current_frame = None

db = sqlite3.connect('events.db', check_same_thread=False)

class DebounceItem(object):
    dt = None
    id = 0

event_types = ['audio', 'video']
event_objects = ['cat', 'dog', 'person', 'face']

events_debouncer = {}
audio_video_record = 0

for event_type in event_types:
    events_debouncer[event_type] = {}
    for event_object in event_objects:
        events_debouncer[event_type][event_object] = DebounceItem()

events_debouncer_sec = 5
event_types_dict = {
    'audio': 'Звук',
    'video': 'Движение'
}

allowed_objects = ['person', 'cat', 'dog', 'face']

emotions = {  # Список эмоций
    0: "Злость",
    1: "Отвращение",
    2: "Страх",
    3: "Счастье",
    4: "Печаль",
    5: "Удивление",
    6: "Нейтральный"
}

labels = [
    "Злость",
    "Отвращение",
    "Страх",
    "Счастье",
    "Печаль",
    "Удивление",
    "Нейтральный"
]

picture_emotions = []
print("Loading emotions images....")
for i in range(7):
    picture_emotions.append(cv2.imread(f'PNG/{i}.png', cv2.IMREAD_COLOR))

displayEmotionIdx = 3
lastEmotionIdx = 6
stats = collections.deque([6] * 60, maxlen=60)
stats.appendleft(3)
stats.appendleft(3)
stats.appendleft(3)
stats.appendleft(3)
stats.appendleft(3)
stats.appendleft(3)

stats.appendleft(2)
stats.appendleft(2)

stats.appendleft(4)
stats.appendleft(4)
stats.appendleft(4)

stats.appendleft(5)

print("Starting streamer...")
os.system("pkill -f \"ustreamer\"")
os.system("ustreamer -s 0.0.0.0 -p 56000 -l -q 80 -w 1 &")
time.sleep(3)

cTime = 0
pTime = 0
updTime = 0
updPeriod = 3
fps = 0

BaseOptions = mp.tasks.BaseOptions
FaceDetector = mp.tasks.vision.FaceDetector
FaceDetectorOptions = mp.tasks.vision.FaceDetectorOptions
VisionRunningMode = mp.tasks.vision.RunningMode

options = FaceDetectorOptions(
    base_options=BaseOptions(model_asset_path='detector.tflite'),
    running_mode=VisionRunningMode.VIDEO,
    min_detection_confidence=0.5
)
face_detector = FaceDetector.create_from_options(options)
# detect_faces = FaceDetection(model_type=FaceDetectionModel.FULL_SPARSE)
# face_detection = mp.solutions.face_detection.FaceDetection(model_selection=0, min_detection_confidence=0.8)

# Object detection
# base_options = python.BaseOptions(model_asset_path='efficientdet_lite2_float32.tflite')
# base_options = python.BaseOptions(model_asset_path='efficientdet_lite0_float32.tflite')
# base_options = python.BaseOptions(model_asset_path='ssd_mobilenet_v2_float32.tflite')

base_options = python.BaseOptions(model_asset_path='ssd_mobilenet_v2_float16.tflite')
# base_options = python.BaseOptions(model_asset_path='efficientdet_lite2_int8.tflite')
# base_options = python.BaseOptions(model_asset_path='efficientdet_lite0_int8.tflite')

options = vision.ObjectDetectorOptions(base_options=base_options, score_threshold=0.5)
object_detector = vision.ObjectDetector.create_from_options(options)

file_folder = 'files/'
file_audio_name = ''
file_video_name = ''
file_photo_name = ''

def date_to_string(dt):
    return datetime.fromtimestamp(dt).strftime("%y-%m-%d_%H-%M-%S")

def generate_filename(id, type, dt):
    return f'{id:05d}_{type}_{date_to_string(dt)}'

def file_photo_save(id, event_dt):
    global file_photo_name
    global current_frame
    if current_frame is None:
        return

    file_name = generate_filename(id, 'photo', event_dt)
    img = cv2.cvtColor(current_frame.copy(), cv2.COLOR_BGR2RGB)
    file_photo_name = file_name + '.jpg'
    cv2.imwrite(file_folder + file_photo_name, np.array(img))

    file_photo_name_thumb = file_name + '_thumb.jpg'
    cv2.resize(img, (256, 192), interpolation=cv2.INTER_CUBIC)
    cv2.imwrite(file_folder + file_photo_name_thumb, np.array(img))

    return file_photo_name

def file_video_save(id, event_dt):
    global file_video_name
    file_video_name = generate_filename(id, 'video', event_dt) + '.mp4'

def file_audio_save(id, event_dt):
    global file_audio_name
    file_audio_name = generate_filename(id, 'audio', event_dt) + '.wav'

def process_event_start(event_type, obj, event_dt):
    global file_photo_name

    title = event_types_dict[event_type]
    now = time.time()
    cursor = db.cursor()
    cursor.execute('INSERT INTO event (title, updated_dt, start_dt, type, object) VALUES (?, ?, ?, ?, ?)',
                   (title, now, event_dt, event_type, obj))
    db.commit()
    inserted_id = cursor.lastrowid
    print(f'new event type#{event_type} obj#{obj} time#{event_dt} id#{inserted_id}')

    file_photo_name = file_photo_save(inserted_id, event_dt)
    cursor.execute('UPDATE event SET file_photo = ? WHERE id = ?', (file_photo_name, inserted_id))
    cursor.close()
    db.commit()
    return inserted_id


def process_event_end(id, event_end_dt, event_type, obj):
    global audio_video_record
    if audio_video_record == 0:
        return None

    now = time.time()
    cursor = db.cursor()
    cursor.execute('UPDATE event SET updated_dt = ?, end_dt = ?, file_audio = ?, file_video = ? WHERE id = ?', (now, event_end_dt, file_audio_name, file_video_name, id))
    cursor.close()
    db.commit()
    print(f'updated id: {id}')


def process_event(event_type, obj, event_dt):
    global audio_video_record
    if audio_video_record == 0:
        return None

    bounce = events_debouncer[event_type][obj]
    if bounce.dt is None:
        bounce.dt = event_dt
        print(f'process_event_start {event_dt}')
        bounce.id = process_event_start(event_type, obj, event_dt)
    else:
        bounce.dt = event_dt
        #print(f'process_event update {bounce.id} - {event_dt}')
    return bounce

@throttle.wrap(5, 1)
def process_events_all():
    for event_type in event_types:
        process_events_for_type(event_type)

def process_events_for_type(event_type, force_end = False):
    for obj in event_objects:
        bounce = events_debouncer[event_type][obj]
        if not bounce.dt is None:
            if force_end or time.time() - bounce.dt >= events_debouncer_sec:
                event_end_dt = bounce.dt
                print(f'process_event_end {event_end_dt}')
                bounce.dt = None
                process_event_end(bounce.id, event_end_dt, event_type, obj)
                bounce.id = 0
def process_events(event_type, objects, event_dt):
    for obj in objects:
        process_event(event_type, obj, event_dt)


def HttpServer():
    print("Http thread started")
    db = sqlite3.connect('events.db', check_same_thread=False)

    app = Flask(
        __name__,
        static_url_path='/files',
        static_folder='files',
    )

    @app.route('/emotion/<idx>', methods=['GET'])
    def get_emotion(idx):
        global displayEmotionIdx
        displayEmotionIdx = int(idx)
        print("New emotion set:")
        print(displayEmotionIdx)
        return jsonify({
            'status': 'Ok'
        })

    def query_db(db, query, args=(), one=False):
        cur = db.cursor()
        cur.execute(query, args)
        r = [dict((cur.description[i][0], value) \
                  for i, value in enumerate(row)) for row in cur.fetchall()]
        cur.close()
        return (r[0] if r else None) if one else r

    @app.route('/get-update/<timestamp>', methods=['GET'])
    def get_update(timestamp):
        data = query_db(db, 'SELECT * FROM event WHERE updated_dt > ? + 1 ORDER BY id', (timestamp,))
        return json.dumps({
            'data': data
        })

    @app.route('/clear-all', methods=['GET'])
    def clear_all(timestamp):
        cur = db.cursor()
        cur.execute('DELETE FROM event')
        return json.dumps({
            'status': 'done'
        })

    @app.route('/stats', methods=['GET'])
    def get_stats():
        global lastEmotionIdx
        global stats
        counter = collections.Counter(stats)
        groupStats = []
        for i in emotions.keys():
            groupStats.append(
                {
                    'emotionIdx': i,
                    'emotionName': emotions[i],
                    'count': counter[i]
                })
        data = {
            'lastEmotionIdx': lastEmotionIdx,
            'lastEmotionName': emotions[lastEmotionIdx],
            'stats': groupStats
        }
        return jsonify(data)

    if __name__ == '__main__':
        app.run(host='0.0.0.0', port=5000)


httpThread = Thread(target=HttpServer, args=[])
print("Starting http thread...")
httpThread.start()

def AudioThread():
    allowed_categories = ['Speech', 'Cat', 'Meow', 'Dog', 'Animal']
    categories_map = {
        'Speech':'person',
        'Animal': 'cat',
        'Cat': 'cat',
        'Meow':'cat',
        'Dog': 'dog',
        'Bark': 'dog'
    }

    need_record = False
    is_recording = False
    record_start = 0
    record_current = 0
    record_end = 0
    TIMEOUT = 5

    def process_result(result: mp.tasks.audio.AudioClassifierResult, timestamp_ms: int):
        nonlocal need_record, record_current
        global file_audio_name
        global audio_video_record

        #if len(result.classifications) > 0 and len(result.classifications[0].categories) > 0:
        for clsf in result.classifications:
            for c in clsf.categories:
                if c.category_name in allowed_categories:
                    event_dt = time.time()
                    need_record = True
                    record_current = time.time()
                    obj = categories_map[c.category_name]
                    #print(obj, c.score)
                    bounce = process_event('audio', obj, event_dt)
                    if bounce is not None and file_audio_name == '':
                        file_audio_name = generate_filename(bounce.id, 'audio', event_dt) + '.wav'

    options = mp.tasks.audio.AudioClassifierOptions(
        base_options=BaseOptions(model_asset_path='yamnet_float32.tflite'),
        running_mode=mp.tasks.audio.RunningMode.AUDIO_STREAM,
        score_threshold=0.4,
        max_results=10,
        result_callback=process_result)

    audio_detector = mp.tasks.audio.AudioClassifier.create_from_options(options)
    t0 = int(time.time() * 1000.0)
    while(True):
        FORMAT = pyaudio.paInt16
        CHANNELS = 1
        RATE = 44100
        CHUNK = 44100

        audio = pyaudio.PyAudio()

        # start Recording
        stream = audio.open(format=FORMAT, channels=CHANNELS,
                            rate=RATE, input=True,
                            frames_per_buffer=CHUNK)
        print("recording...")
        frames = []

        #for i in range(0, int(RATE / CHUNK * RECORD_SECONDS)):
        prevData = None
        data = None
        while True:
            t = int(time.time() * 1000.0) - t0
            if data is not None:
                prevData = data
            data = stream.read(CHUNK)

            if audio_video_record == 0:
                continue

            audio_data = mp.tasks.components.containers.AudioData.create_from_array(np.frombuffer(data, dtype=np.int16).astype(float) / np.iinfo(np.int16).max, 44100)
            audio_detector.classify_async(audio_data, t)

            if need_record:
                # Start recording
                if not is_recording:
                    print("Start recording")
                    is_recording = True
                    record_start = time.time()
                    if prevData is not None:
                        frames.append(prevData)

                frames.append(data)

            if is_recording and (time.time() - record_current) > TIMEOUT:
                # Stop recording and save file
                need_record = False
                is_recording = False
                record_end = time.time()
                global file_folder, file_audio_name
                file_name = file_folder + file_audio_name
                print("Finished recording, write to file " + file_name)
                wave_file = wave.open(file_name, 'wb')
                wave_file.setnchannels(CHANNELS)
                wave_file.setsampwidth(audio.get_sample_size(FORMAT))
                wave_file.setframerate(RATE)
                wave_file.writeframes(b''.join(frames))
                wave_file.close()
                frames = []
                process_events_for_type('audio', True)
                file_audio_name = ''
                need_record = False
                is_recording = False

        # stop Recording
        stream.stop_stream()
        stream.close()
        audio.terminate()

        #waveFile = wave.open(WAVE_OUTPUT_FILENAME, 'wb')
        #waveFile.setnchannels(CHANNELS)
        #waveFile.setsampwidth(audio.get_sample_size(FORMAT))
        #waveFile.setframerate(RATE)
        #waveFile.writeframes(b''.join(frames))
        #waveFile.close()

        #audio_data = mp.tasks.components.containers.AudioData.create_from_array(buffer.astype(float) / np.iinfo(np.int16).max, 44100)
        #audio_detector.classify_async(audio_data, t)

audioThread = Thread(target=AudioThread, args=[])
print("Starting audio thread...")
audioThread.start()


class CameraBufferCleanerThread(threading.Thread):
    def __init__(self, camera, name='camera-buffer-cleaner-thread'):
        self.camera = camera
        self.last_frame = None
        super(CameraBufferCleanerThread, self).__init__(name=name)
        self.start()

    def run(self):
        while True:
            ret, self.last_frame = self.camera.read()


print("Starting main thread...")
os.environ['DISPLAY'] = ':0'


# limit to 3 calls
# allow more every 1 second
@throttle.wrap(1 / 10, 1)
def read_camera(cap_cleaner):
    t = time.time()
    # success, image = cap.read()
    image = cap_cleaner.last_frame
    success = image is not None
    # cap.retrieve(image)
    # success = cap.grab()
    if not success:
        print("Ignoring empty camera frame.")
        # If loading a video, use 'break' instead of 'continue'.
        return throttle.fail

    image = cv2.rotate(image, cv2.ROTATE_180)
    image = cv2.cvtColor(image, cv2.COLOR_RGB2BGR)

    delta = (time.time() - t) * 1000
    #print(f'Reading camera {delta:.0f}ms')
    return image


@throttle.wrap(1 / 20, 1)
def detect(frame, detect_objects=True):
    t = time.time()
    detection = False
    cropped_image = []
    cropped_image2x = False

    h, w, c = tuple(frame.shape)
    # image = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
    mp_image = mp.Image(image_format=mp.ImageFormat.SRGB, data=frame)
    results = face_detector.detect_for_video(mp_image, int(time.time_ns() / 1000))

    # image.flags.writeable = True
    # image = cv2.cvtColor(image, cv2.COLOR_RGB2BGR)

    if results.detections:
        max_w = 0
        for idx, d in enumerate(results.detections):
            bb = d.bounding_box
            if max_w < bb.width:
                max_w = bb.width
                detection = d

    if detection:
        bb = detection.bounding_box
        bb.origin_x -= int(bb.width * 0.1)
        bb.origin_y -= int(bb.height * 0.15)
        bb.width = int(bb.width * 1.2)
        bb.height = int(bb.height * 1.15)

        x1, y1 = max(0, int(bb.origin_x)), max(0, int(bb.origin_y))
        w1, h1 = max(0, int(bb.width)), max(0, int(bb.height))
        wa, ha = max(0, h1 - w1), max(0, w1 - h1)
        w1 += wa
        h1 += ha
        x2, y2 = min(w - 1, x1 + w1), min(h - 1, y1 + h1)

        if x2 > 0 and y2 > 0:
            cropped_image = frame[y1:y2, x1:x2]
            cropped_image = cv2.cvtColor(cropped_image, cv2.COLOR_BGR2GRAY)
            # cv2.resize(frame[y1:y2, x1:x2], (48, 48), cropped_image, 0, 0, interpolation=cv2.INTER_LANCZOS4)
            cropped_image = cv2.resize(cropped_image, (48, 48), interpolation=cv2.INTER_LANCZOS4)
        else:
            detection = False

    if detect_objects and not detection:
        detection_result = object_detector.detect(mp_image)
        detection_result.detections = list(filter(lambda x: x.categories[0].category_name in allowed_objects, detection_result.detections))
    else:
        detection_result = False

    delta = (time.time() - t) * 1000
    #print(f'Detection {delta:.0f}ms')
    return detection, cropped_image, detection_result


def init():
    cap = cv2.VideoCapture("http://127.0.0.1:56000/stream")
    cap.set(cv2.CAP_PROP_POS_FRAMES, 0)
    cap.set(cv2.CAP_PROP_BUFFERSIZE, 0)
    # cap = cv2.VideoCapture(0)
    # cap.set(cv2.CAP_PROP_FRAME_WIDTH, 1280)
    # cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 960)
    cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
    cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)

    bgImg = cv2.imread('BMO_Face_Smile.png', cv2.IMREAD_COLOR)
    blackImg = cv2.imread('black.png', cv2.IMREAD_COLOR)

    cv2.namedWindow("canvas", cv2.WND_PROP_FULLSCREEN)
    cv2.setWindowProperty("canvas", cv2.WND_PROP_FULLSCREEN, cv2.WINDOW_FULLSCREEN)

    #cv2.namedWindow("canvas", cv2.WND_PROP_AUTOSIZE)
    #cv2.setWindowProperty("canvas", cv2.WND_PROP_AUTOSIZE, cv2.WINDOW_AUTOSIZE)
    #cv2.resizeWindow("canvas", 1024, 768)

    cv2.imshow("canvas", bgImg)

    # if cap.isOpened():
    #     success, image = cap.read()
    #     if not success:
    #         print("Ignoring empty camera frame.")
    #         # If loading a video, use 'break' instead of 'continue'.
    # else:
    #     print("Can't open camera")
    #     quit()

    print("Loading model....")

    # with open("model.json", "r") as json_file:
    #    model_json = json_file.read()

    # model = tf.keras.models.model_from_json(model_json)
    # model = False
    model = tf.keras.models.load_model('model_v2a.keras')
    print(model.summary())
    cap_cleaner = CameraBufferCleanerThread(cap)

    return cap, cap_cleaner, model


def detect_emotion(model, image):
    t = time.time()
    predict = model.predict(np.array([image]), verbose=0, steps=1)
    emI = np.argmax(predict, axis=1)[0].item()
    delta = (time.time() - t) * 1000
    print(f'Emotion detection {delta:.0f}ms')
    return emI


def draw_detection(image, detection):
    # Draw bounding_box
    bbox = detection.bounding_box
    start_point = bbox.origin_x, bbox.origin_y
    end_point = bbox.origin_x + bbox.width, bbox.origin_y + bbox.height
    cv2.rectangle(image, start_point, end_point, (0, 255, 0), 1)


MARGIN = 10  # pixels
ROW_SIZE = 10  # pixels
FONT_SIZE = 2
FONT_THICKNESS = 1
TEXT_COLOR = (0, 255, 0)  # red

categories = {
    "person": "человек",
    "cat": "кошка",
    "dog": "собака"
}


def visualize(
        image,
        detection_result
) -> np.ndarray:
    """Draws bounding boxes on the input image and return it.
    Args:
      image: The input RGB image.
      detection_result: The list of all "Detection" entities to be visualize.
    Returns:
      Image with bounding boxes.
    """
    if detection_result == False:
        return image

    for detection in detection_result.detections:
        category = detection.categories[0]
        category_name = category.category_name
        probability = round(category.score, 2)

        if not category_name in ['person', 'cat', 'dog']:
            continue

        # Draw bounding_box
        bbox = detection.bounding_box
        start_point = bbox.origin_x, bbox.origin_y
        end_point = bbox.origin_x + bbox.width, bbox.origin_y + bbox.height
        cv2.rectangle(image, start_point, end_point, TEXT_COLOR, 3)

        # Draw label and score
        result_text = category_name + ' (' + str(probability) + ')'
        text_location = (MARGIN + bbox.origin_x,
                         MARGIN + ROW_SIZE + bbox.origin_y)
        cv2.putText(image, result_text, text_location, cv2.FONT_HERSHEY_PLAIN,
                    FONT_SIZE, TEXT_COLOR, FONT_THICKNESS)

    return image


def main():
    # global pTime
    # global cTime
    # global updTime
    # global updPeriod
    # global fps
    global lastEmotionIdx
    global displayEmotionIdx
    global current_frame
    global audio_video_record

    mode = 0
    modeMax = 3
    autoDetect = 0

    cap, cap_cleaner, model = init()
    img = picture_emotions[displayEmotionIdx].copy()
    detection = False
    detection_prev = detection
    em = ""

    pTime = time.time()

    while cap.isOpened():
        camera_result = read_camera(cap_cleaner)
        if camera_result is throttle.fail:
            continue
        else:
            image = camera_result
        current_frame = image.copy()

        key = cv2.waitKey(5) & 0xFF
        old_mode = mode
        if key == 81:
            mode -= 1
            if mode < 0:
                mode = modeMax
            print(f'mode={mode}')

        if key == 83:
            mode += 1
            if mode > modeMax:
                mode = 0
            print(f'mode={mode}')

        if key == 82:
            if autoDetect == 1:
                autoDetect = 0
            else:
                autoDetect = 1
            print(f'autoDetect={autoDetect}')

        if key == 84:
            if audio_video_record == 0:
                audio_video_record = 1
            else:
                audio_video_record = 0
            print(f'audio_video_record={audio_video_record}')

        mode_changed = old_mode != mode

        if key == 27 or key == 13:
            break

        # Flip the image horizontally for a selfie-view display.
        emI = 6

        now = time.time()
        if mode == 0:
            img = picture_emotions[displayEmotionIdx].copy()
        elif mode == 1:
            img = picture_emotions[emI].copy()
        else:
            face_result = detect(image, autoDetect != 1)
            if not face_result is throttle.fail:
                detection, cropped_image, objects = face_result

                if objects:
                    for obj in objects.detections:
                        process_event('video', obj.categories[0].category_name, now)

                if detection:
                    process_event('video', 'face', now)
                    detection_prev = detection
            else:
                detection = False

            if mode == 2:
                if detection:
                    draw_detection(image, detection)
                elif detection_prev:
                    draw_detection(image, detection_prev)
                img = visualize(image, objects)
            if mode == 3:
                if detection:
                    img = cropped_image
            current_frame = img.copy()

        if (key == 32 or autoDetect == 1) and detection:
            emI = detect_emotion(model, cropped_image)
            em = emotions[lastEmotionIdx]
            print(em)
            lastEmotionIdx = emI
            stats.appendleft(lastEmotionIdx)
            # print(stats)

        img = cv2.flip(img, 1)

        if mode >= 2:
            cv2.putText(img, em, (20, 60), cv2.FONT_HERSHEY_COMPLEX, 1, (255, 255, 255), 2)

        cTime = time.time()
        fps = 1 / (cTime - pTime)
        pTime = cTime

        if mode == 2:
            cv2.putText(img, f'FPS: {int(round(fps, 0)):2d}', (20, 40), cv2.FONT_HERSHEY_PLAIN, 2, (0, 255, 0), 2)

        img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
        cv2.imshow('canvas', cv2.resize(img, (1024, 786), interpolation=cv2.INTER_CUBIC))

        process_events_all()
    cap.release()

main()

httpThread.do_run = False
audioThread.do_run = False
# streamer.terminate()
os.system("pkill -f \"ustreamer\"")
