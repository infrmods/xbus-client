#!/usr/bin/env python
import os
from setuptools import setup


with open(os.path.join(os.path.dirname(__file__), 'requirements.txt')) as f:
    requirements = [l for l in f.readlines() if l.strip()]


setup(name='xbus',
      version='0.1.5',
      description='python client for xbus',
      author='Jiong Du',
      author_email='londevil@gmail.com',
      url='https://github.com/infrmods/xbus-client',
      license='Apache 2.0',
      packages=['xbus'],
      install_requires=requirements)
