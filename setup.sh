#!/bin/bash

# Copyright 2015-2017 Stanford University
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http:#www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#
# Get current directory
#

DIR="$(pwd)"


#
# Set up query oracles
#

# set up folders
mkdir prog-query
cd prog-query

# sed
mkdir sed
cd sed
tar xf ../../data/prog/sed-4.2.2.tar.gz
cd sed-4.2.2
./configure
make
cd $DIR/prog-query

# grep
mkdir grep
cd grep
tar xf ../../data/prog/grep-2.23.tar.xz
cd grep-2.23
./configure
make
cd $DIR/prog-query

# flex
mkdir flex
cd flex
tar xf ../../data/prog/flex-2.6.0.tar.gz
cd flex-2.6.0
./configure
make
cd $DIR/prog-query

# xml
mkdir xml
cd xml
tar xf ../../data/prog/libxml2-2.9.2.tar.gz
cd libxml2-2.9.2
./configure
make
cd $DIR/prog-query

# python
mkdir python
cd python
tar xf ../../data/prog/Python-2.7.10.tgz
cd Python-2.7.10
./configure
make
cd $DIR/prog-query

# js
mkdir ffjs
cd ffjs
tar xf ../../data/prog/mozjs-38.2.1.rc0.tar.bz2
cd mozjs-38.0.0/js/src
./configure
make
cd $DIR/prog-query

# end in root directory
cd $DIR


#
# Set up coverage oracles
#

# set up folders
mkdir prog-gcov
cd prog-gcov

# sed
mkdir sed
cd sed
tar xf ../../data/prog/sed-4.2.2.tar.gz
cd sed-4.2.2
CFLAGS="-fprofile-arcs -ftest-coverage" CXXFLAGS="-fprofile-arcs -ftest-coverage" LDFLAGS="-fprofile-arcs" ./configure
make
cd $DIR/prog-gcov

# grep
mkdir grep
cd grep
tar xf ../../data/prog/grep-2.23.tar.xz
cd grep-2.23
CFLAGS="-fprofile-arcs -ftest-coverage" CXXFLAGS="-fprofile-arcs -ftest-coverage" LDFLAGS="-fprofile-arcs" ./configure
make
cd $DIR/prog-gcov

# flex
mkdir flex
cd flex
tar xf ../../data/prog/flex-2.6.0.tar.gz
cd flex-2.6.0
CFLAGS="-fprofile-arcs -ftest-coverage" CXXFLAGS="-fprofile-arcs -ftest-coverage" LDFLAGS="-fprofile-arcs" ./configure
make
cd $DIR/prog-gcov

# xml
mkdir xml
cd xml
tar xf ../../data/prog/libxml2-2.9.2.tar.gz
cd libxml2-2.9.2
./configure --disable-shared --with-coverage
make
cd $DIR/prog-gcov

# python
mkdir python
cd python
tar xf ../../data/prog/Python-2.7.10.tgz
cd Python-2.7.10
CFLAGS="-fprofile-arcs -ftest-coverage" CXXFLAGS="-fprofile-arcs -ftest-coverage" LDFLAGS="-fprofile-arcs" ./configure
make
cd $DIR/prog-gcov

# spidermonkey gcov
mkdir ffjs
cd ffjs
tar xf ../../data/prog/mozjs-38.2.1.rc0.tar.bz2
cd mozjs-38.0.0/js/src
CFLAGS="-fprofile-arcs -ftest-coverage" CXXFLAGS="-fprofile-arcs -ftest-coverage" LDFLAGS="-fprofile-arcs" ./configure
make
cd $DIR/prog-gcov

# end in root directory
cd $DIR
