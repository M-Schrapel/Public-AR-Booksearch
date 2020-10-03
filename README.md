# Augmenting Public Bookcases to Support Book Sharing

This repository provides our code used for the paper: 
[`Augmenting Public Bookcases to Support Book Sharing`](https://hci.uni-hannover.de/papers/Schrapel2020_Augmenting_Public_Bookcases.pdf)
Our contribution includes:
- **Dataset**: In folder Bookcase_Images and Spines you find the corresponding images.
- **Application**: In folder android_application you find the Android application that was created by Thilo Schulz during his Master's thesis. The app is optimized to work on a Samsung Galaxy S9 smartphone.


## Citation
If you use our app or dataset in your projects, please use the following BibTeX citation:
```
@inproceedings{Schrapel:2020:Bookcases,
  title = {Augmenting Public Bookcases to Support Book Sharing},
  booktitle = {Proceedings of the 22th international conference on Human computer interaction with mobile devices and services},
  year = {2020},
  doi = {3379503.3403542},
  series = {MobileHCI '20},
  author = {Schrapel, Maximilian and Schulz, Thilo and Rohs, Michael}, 
}
```

## Adding images to the dataset

Put your book spine images into the covers folder.
Then open the file book_spine_data_no_color.csv.
Add title,subtitle,author,publisher,genre,filename
The filename is the name of your added book spine image.
You can leave unused fields blank. 

