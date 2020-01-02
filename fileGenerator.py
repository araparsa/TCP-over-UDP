# -*- coding: utf-8 -*-
# @Author: arman
# @Date:   2020-01-02 20:08:24
# @Last Modified by:   arman
# @Last Modified time: 2020-01-02 20:16:10
import random

def generate(fileSize):
	f = open("file.txt", "a")
	for i in range(int(fileSize/4)):
		f.write(str(random.randint(0, 99)))
	f.close()

def main():
	generate(10000)

if __name__ == '__main__':
	main()