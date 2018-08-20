SRC=pgjdbc disclaimer intro setup classpath prepare your-database use	\
load connect ssl ssl-client ssl-factory query statement resultset	\
update ddl java8-date-time callproc binary-data escapes			\
outer-joins-escape escapes-datetime escaped-functions ext geometric	\
largeobjects listennotify server-prepare replication arrays thread	\
datasource ds-cpds ds-ds tomcat jndi logging reading

SRC_MD=$(patsubst %, cache/%.md, $(SRC))

pgjdbc.pdf : $(SRC_MD)
	2ndq-pandoc $(SRC_MD) --filter=tools/single-pdf.py
	mv cache/pgjdbc.pdf $@

cache/%.md : tools/make-%.sh
	bash $< >$@

cache/%.md : head/%.md
	bash tools/preprocess.sh $< $*

src: $(SRC_MD)

clean:
	rm -f cache/*
