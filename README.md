GLADE
=====

GLADE is a tool for automatically synthesizing program input grammars, i.e., a context-free grammar encoding the language of valid inputs for a target program. GLADE requires two inputs: (i) an query oracle that responds whether a given input is valid for the target program, and (ii) a set of examples of valid inputs, known as seed inputs. From these inputs, GLADE produces a grammar that can be sampled to produce new program inputs.

Table of Contents
=====
0. Prerequisites
1. Downloading and Building GLADE
2. Running GLADE
3. Notes
4. Contact

Prerequisites
=====

GLADE has been tested on Ubuntu 64-bit 16.04.1 and on Mac OS X 10.9.5, but should work on typical Linux installations. GLADE requires Java 1.7 or above, and building GLADE requires Apache Ant. The build process has specifically been tested on OpenJDK version 1.8.0_91 and Apache Ant version 1.9.7. The example programs that come with GLADE should be self contained, except flex requires GNU M4, which on Ubuntu can be installed using

    $ sudo apt-get install m4

GLADE can be built without GNU M4, but running the flex example will fail (the remaining programs should run without issue).

Downloading and Building GLADE
=====

The GLADE source code is available on GitHub under the Apache Software License version 2.0 at https://github.com/obastani/glade. To check out the GLADE source code repository, run:

    $ git clone https://github.com/obastani/glade.git

To build GLADE, run:

    $ cd glade
    $ ant

Doing so should produce a jar file named `glade.jar`.

The GLADE code is currently set up to synthesize grammars for the following programs: GNU sed, GNU grep, flex, xml, the standard Python interpreter, and the Mozilla SpiderMonkey Javascript interpreter. GNU bison and the Ruby interpreter are unavailable due to technical difficulties making their installations portable.

To set up the example programs that come with GLADE on Ubuntu, run:

    $ ./setup.sh

This command should work for typical Linux distributions. If setting up GLADE on Mac OS X, instead run

    $ ./setup_osx.sh

To uninstall GLADE, run:

    $ ant clean
    
To uninstall the example programs that come with GLADE, run:

    $ ./cleanup.sh

Running GLADE
=====

Currently, GLADE can synthesize a handwritten grammar using either RPNI, L-Star, or the GLADE algorithm. To do so, run

    $ java -jar glade.jar learn-handwritten <algorithm> <grammar>

The available values of `<algorithm>` are `rpni`, `lstar`, `glade`, and `glade-p1`. The last option omits phase two generalization steps from the GLADE algorithm. The available values of `<grammar>` are `url`, `grep`, `lisp`, and `xml`. The handwritten grammars are stored in `data/handwritten`. For example, running:

    $ java -jar glade.jar learn-handwritten glade xml

synthesizes a grammar for the handwritten XML grammar using the full GLADE algorithm.

To synthesize a grammar for an example program using GLADE, run:

    $ java -jar glade.jar learn-program <program>

The available values of `<program>` are `sed`, `grep`, `flex`, `xml`, `python`, and `js`. For example, running:

    $ java -jar glade.jar learn-program sed

synthesizes a grammar for GNU sed.

The seed inputs given to GLADE as examples for each of these programs are stored in `data/inputs-train/<program>/`. Learned grammars are stored (in serialized form, not human readable) in the folder `data/grammars/<program>/`. A grammar is generated for each seed input, as well as a grammar `all.gram` learned from all seed inputs.

Once a grammar has been synthesized for a program (in particular, a file `data/grammars/<program>/all.gram` has been generated), it can be used to randomly generate new inputs by running:

    $ java -jar glade.jar fuzz-program <program>

This command runs both the naïve fuzzer and GLADE's grammar-based fuzzer, and reports the normalized incremental coverage as defined in the paper. For example, running:

    $ java -jar glade.jar fuzz-program sed

reports the normalized incremental coverage on `sed`. Note that the file `data/grammars/<program>/all.gram` must be available, or this mode will fail.

Notes
=====

- Flex, sed, grep, and xml inputs are primarily obtained from the respective distributions.
- Python inputs are obtained from https://wiki.python.org/moin/SimplePrograms.
- Javascript inputs are obtained from http://www.cs.princeton.edu/courses/archive/fall10/cos109/JS_topost.

Contact
=====

For questions, feel free to contact `obastani@cs.stanford.edu`.
