**Autobahn**|Android documentation is generated using [Sphinx](http://sphinx-doc.org/).

To install Sphinx:

	pip install -U sphinx

To install [javasphinx](http://bronto.github.io/javasphinx/), install [lxml](https://pypi.python.org/pypi/lxml/) and then:
 
	pip install -U javasphinx

To generate the docs:

	make html

> This will run `javasphinx-apidoc -u -o _gen ../Autobahn/src/` under the hood to first generate RST files from Javadoc.
> 

To cleanup

	make clean
