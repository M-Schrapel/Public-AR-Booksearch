# -*- coding: utf-8 -*-
import csv
import time
from PIL import Image
from enum import Enum

HIST_LENGTH = 8
INDEX_COLOR = 6
filename_no_color = "book_spine_data_no_color.csv"
filename_result = "book_spine_data.csv"
path_result = "android_application/app/src/main/assets/"
path_images = "Spines/"

class HistColor(Enum):
    BLACK = 0
    WHITE_GRAY = 1
    RED = 2
    YELLOW = 3
    GREEN = 4
    CYAN = 5
    BLUE = 6
    MAGENTA = 7

def extractColor(filename):
    with Image.open(path_images + filename + ".png", "r") as img:
        pix_val = list(img.getdata())
        # transparente Pixel (pix[-1]) sollen nicht betrachtet werden
        pix_val_cleaned = [pix for pix in pix_val if pix[-1] != 0]
        hist = [0] * HIST_LENGTH
        size = len(pix_val_cleaned)
        for rgb in pix_val_cleaned:
            #print(f"{rgb}")
            r, g, b = [i / 255 for i in rgb[:3]]
            #print(f"{r}")
            #print(f"{g}")
            #print(f"{b}")
            maximum = max(r, g, b)
            minimum = min(r, g, b)
            
            value = maximum
            saturation = 0 if maximum == 0 else (maximum - minimum) / maximum
            #print(f"value = {value}")
            #print(f"saturation = {saturation}")
            if value < 0.15:
                hist[HistColor.BLACK.value] += 1
                continue
            if saturation < 0.3:
                hist[HistColor.WHITE_GRAY.value] += 1
                continue
    
            hue = 0
            if maximum == minimum:
                hue = 0
            elif maximum == r:
                hue = 60 * (g - b) / (maximum - minimum)
            elif maximum == g:
                hue = 60 * (2 + (b - r) / (maximum - minimum))
            else:
                hue = 60 * (4 + (r - g) / (maximum - minimum))
                
            if hue < 0:
                hue += 360   
            #print(f"hue = {hue}\n")
    
            if hue <= 40 or hue > 320:
                hist[HistColor.RED.value] += 1
                continue
            if hue <= 80:
                hist[HistColor.YELLOW.value] += 1
                continue
            if hue <= 160:
                hist[HistColor.GREEN.value] += 1
                continue
            if hue <= 200:
                hist[HistColor.CYAN.value] += 1
                continue
            if hue <= 280:
                hist[HistColor.BLUE.value] += 1
            else:
                hist[HistColor.MAGENTA.value]+= 1
        
    for i in range(HIST_LENGTH):
        hist[i] /= size
    #print(f"{hist}") 
    return hist

start_time = time.time()

with open(filename_no_color, mode="r") as read_file:
    with open(path_result + filename_result, mode="w", newline='') as write_file:
        csv_reader = csv.reader(read_file)
        csv_writer = csv.writer(write_file)
        line_count = 0
        for row in csv_reader:
            if line_count == 0:
                row[INDEX_COLOR] = "color"
                line_count += 1
            else:
                filename = row[INDEX_COLOR]
                row[INDEX_COLOR] = extractColor(filename)
            print(f"{row}\n")
            csv_writer.writerow(row)
            
end_time = time.time()
minutes, seconds = divmod(end_time - start_time, 60)
print(f"Finished after {int(minutes)}m {int(seconds)}s.")