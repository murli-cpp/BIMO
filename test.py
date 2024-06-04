import json
import subprocess
import threading
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

emotions = { # Список эмоций
    0:"Злость",
    1:"Отвращение",
    2:"Страх",
    3:"Счастье",
    4:"Печаль",
    5:"Удивление",
    6:"Нейтральный"
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

displayEmotionIdx = 3
lastEmotionIdx = 6
stats = collections.deque([6]*60, maxlen=60)
cropped_image = []

print("Starting streamer...")
os.system("pkill -f \"ustreamer\"")
os.system("ustreamer -s 0.0.0.0 -p 56000 -l -q 50 &")
time.sleep(2)

def HttpServer():
    print("Http thread started")

    app = Flask(__name__)

    @app.route('/emotion/<idx>', methods=['GET'])
    def get_emotion(idx):
        global displayEmotionIdx
        displayEmotionIdx = int(idx)
        print("New emotion set:")
        print(displayEmotionIdx)
        return jsonify({
            'status': 'Ok'
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

cap = cv2.VideoCapture("http://127.0.0.1:56000/stream")
cap.set(cv2.CAP_PROP_POS_FRAMES, 0)
cap.set(cv2.CAP_PROP_BUFFERSIZE, 0)
#cap = cv2.VideoCapture(0)
#cap.set(cv2.CAP_PROP_FRAME_WIDTH, 1280)
#cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 960)
cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)
cap_cleaner = CameraBufferCleanerThread(cap)

bgImg = cv2.imread('BMO_Face_Smile.png', 0)
blackImg = cv2.imread('black.png', 0)
cv2.namedWindow("canvas", cv2.WND_PROP_FULLSCREEN)
cv2.setWindowProperty("canvas", cv2.WND_PROP_FULLSCREEN, cv2.WINDOW_FULLSCREEN)
#cv2.setWindowProperty("canvas", cv2.WND_PROP_AUTOSIZE, cv2.WINDOW_AUTOSIZE)
cv2.imshow("canvas", bgImg)
mode = 0
modeMax = 3
autoDetect = 0

# if cap.isOpened():
#     success, image = cap.read()
#     if not success:
#         print("Ignoring empty camera frame.")
#         # If loading a video, use 'break' instead of 'continue'.
# else:
#     print("Can't open camera")
#     quit()

mp_face_detection = mp.solutions.face_detection
mp_drawing = mp.solutions.drawing_utils

pTime = 0
updTime = 0
updPeriod = 3

print("Loading model....")

with open("model.json", "r") as json_file:
    model_json = json_file.read()

model = tf.keras.models.model_from_json(model_json)
print("Loading model weights....")

model.load_weights("model.h5")
print(model.summary())

picture_emotions = []

print("Loading emotions images....")
for i in range(7):
    picture_emotions.append(cv2.imread(f'PNG/{i}.png', 0))

with mp_face_detection.FaceDetection(model_selection=0, min_detection_confidence=0.8) as face_detection:
    while cap.isOpened():
        #success, image = cap.read()
        image = cap_cleaner.last_frame
        success = image is not None
        #cap.retrieve(image)
        #success = cap.grab()
        if not success:
            print("Ignoring empty camera frame.")
            # If loading a video, use 'break' instead of 'continue'.
            continue
        image = cv2.rotate(image, cv2.ROTATE_180)
        #image.flags.writeable = False
        image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
        h, w, c = tuple(image.shape)
        results = face_detection.process(image)

        cTime = time.time()
        d = cTime - pTime
        fps = 1 / d
        pTime = cTime

        image.flags.writeable = True
        image = cv2.cvtColor(image, cv2.COLOR_RGB2BGR)
        maxW = 0
        detection = False
        cropped_image2x = False

        if results.detections:
            for idx, d in enumerate(results.detections):
                bb = d.location_data.relative_bounding_box
                if maxW < bb.width:
                    maxW = bb.width
                    detection = d

        if detection:
            bb = detection.location_data.relative_bounding_box

            bb.xmin -= bb.width * 0.1
            bb.ymin -= bb.height * 0.15
            bb.width *= 1.2
            bb.height *= 1.15

            x1, y1 = max(0, int(bb.xmin*w)), max(0, int(bb.ymin*h))
            w1, h1 = max(0, int(bb.width*w)), max(0, int(bb.height*h))
            wa, ha = max(0, h1 - w1), max(0, w1 - h1)
            w1 += wa
            h1 += ha
            x2, y2 = min(w-1, x1 + w1), min(h-1, y1 + h1)

            if x2 > 0 and y2 > 0:
                cropped_image = image[y1:y2, x1:x2]
                cropped_image = cv2.resize(cropped_image, (48, 48), interpolation=cv2.INTER_CUBIC)
                cropped_image = cv2.cvtColor(cropped_image, cv2.COLOR_BGR2GRAY)

                mp_drawing.draw_detection(image, detection)
                cropped_image2x = cv2.resize(cropped_image, (cropped_image.shape[1] * 10, cropped_image.shape[0] * 10), interpolation=cv2.INTER_NEAREST)
                #cv2.imshow('Face #1', cropped_image2x)
                #image = np.concatenate((image, cropped_image2x), axis=1)
                #image = cv2.hconcat([image, cropped_image2x])
                if cTime - updTime >= updPeriod:
                    updTime = cTime
                    #predict = model.predict(np.array([cropped_image]))
                    #print(emotions[np.argmax(predict, axis=1)[0]])

        # Flip the image horizontally for a selfie-view display.
        emI = 6

        if mode == 0:
            img = picture_emotions[displayEmotionIdx].copy()
        if mode == 1:
            img = picture_emotions[emI].copy()
        if mode == 2:
            img = cv2.flip(image, 1)
        if mode == 3:
            if detection and cropped_image.any():
                img = cropped_image2x
            else:
                img = image

        cv2.putText(image, f'FPS: {round(fps, 1):.1f}', (20, 440), cv2.FONT_HERSHEY_PLAIN, 2, (0, 255, 0), 2)

        key = cv2.waitKey(5) & 0xFF

        if key == 27 or key == 13:
            break

        em = ''
        if (key == 32 or autoDetect == 1) and cropped_image.any():
            predict = model.predict(np.array([cropped_image]))
            emI = np.argmax(predict, axis=1)[0].item()
            em = emotions[emI]
            print(em)
            lastEmotionIdx = emI
            stats.appendleft(lastEmotionIdx)
            print(stats)
            if mode == 1:
                img = picture_emotions[emI]
            else:
                cv2.putText(img, em, (20, 60), cv2.FONT_HERSHEY_COMPLEX, 2, (255, 255, 255), 2)

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
            autoDetect = 1
            print(f'autoDetect={autoDetect}')

        if key == 84:
            autoDetect = 0
            print(f'autoDetect={autoDetect}')

        cv2.imshow('canvas', img)

cap.release()
httpThread.do_run = False
#streamer.terminate()
os.system("pkill -f \"ustreamer\" &")
