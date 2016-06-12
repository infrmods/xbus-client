#!/usr/bin/env python

from setuptools import setup


with open('requirements.txt') as f:
    requirements = [l for l in f.readlines() if l.strip()]


setup(name='xbus',
      version='0.1',
      description='python client for xbus',
      author='Jiong Du',
      author_email='londevil@gmail.com',
      url='github.com/infrmods/xbus-client',
      license='Apache 2.0',
      packages=['xbus'],
      install_requires=requirements)
