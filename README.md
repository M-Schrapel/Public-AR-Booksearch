#  :books: Augmenting Public Bookcases to Support Book Sharing

![Teaser Image](/Bookcase_teaser.png)


This repository provides our code used for the paper: 
[`Augmenting Public Bookcases to Support Book Sharing`](https://hci.uni-hannover.de/papers/Schrapel2020_Augmenting_Public_Bookcases.pdf)

Our contribution includes:
- **Dataset**: [Download the dataset](https://drive.google.com/file/d/1ORzsnQ9cH193VQWvrV8S2hC-qPxRQSvA/view?usp=sharing)
- **Application**: In folder ``android_application`` you find the Android application that was created by Thilo Schulz during his Master's thesis. The app is optimized for use with a Samsung Galaxy S9 smartphone.

The dataset and repository is licensed under MIT licsense

## Dataset
Our [dataset](https://drive.google.com/file/d/1ORzsnQ9cH193VQWvrV8S2hC-qPxRQSvA/view?usp=sharing) includes:
<details>
  <summary>Book spine images</summary>
  <p>.png images of book spines in different sizes and various conditions</p>
</details>
<details>
  <summary>Bookcase images</summary>
  <p>Subfolders B0 to B8 provide images from public bookcases<br>
  Subfolder Lab shows 100 different arranged bookcase images from our dataset in a laboratory</p>
</details>

After the download, add all images to the corresponding folders. The download is not neccessary for installing the app. 
The .csv-file located in ``android_application/app/src/main/assets/book_spine_data.csv`` already provides precalculated features.
The dataset is licsened under MIT licsense.

### Adding images to the dataset

- [Download the dataset](https://drive.google.com/file/d/1ORzsnQ9cH193VQWvrV8S2hC-qPxRQSvA/view?usp=sharing)
- Put the downloaded images in the corresponding folders
- Put your book spine .png images into the ``Spines`` folder
- Then open the file ``book_spine_data_no_color.csv``
- Add title,subtitle,author,publisher,genre,filename
- The filename is the name of your added book spine image
- You can leave unused fields blank but you should at least enter the book title.
- If you want to recalcuate all features, simply run in a terminal `python create_data_set.py`
- If you want to add books change the target .csv-filename ``filename_result`` in ``create_data_set.py`` and run the file

## Citation
If you use our app and/or dataset in your projects, please use the following BibTeX citation:
```
@inproceedings{10.1145/3379503.3403542,
author = {Schrapel, Maximilian and Schulz, Thilo and Rohs, Michael},
title = {Augmenting Public Bookcases to Support Book Sharing},
year = {2020},
isbn = {9781450375160},
publisher = {Association for Computing Machinery},
address = {New York, NY, USA},
url = {https://doi.org/10.1145/3379503.3403542},
doi = {10.1145/3379503.3403542},
booktitle = {22nd International Conference on Human-Computer Interaction with Mobile Devices and Services},
articleno = {11},
numpages = {11},
keywords = {Sharing Economy, Computer Vision, Sharing Community, Design for Sharing, Augmented Reality, Mobile Interaction},
location = {Oldenburg, Germany},
series = {MobileHCI '20}
}
```

##
![HCI Group](/Institute.png)

This repository is provided by the Human-Computer Interaction Group at the University Hannover, Germany. For additional details, see our [MobileHCI'20 paper](https://hci.uni-hannover.de/papers/Schrapel2020_Augmenting_Public_Bookcases.pdf). 
The dataset and code is licsened under MIT license. For inquiries, please contact maximilian.schrapel@hci.uni-hannover.de
<br>:books: :heavy_plus_sign: :iphone: :arrow_right: :heart:
