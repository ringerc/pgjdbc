#! /usr/bin/env python

from __future__ import print_function

import mimetypes
import subprocess
import os
import sys
import re
from pandocfilters import toJSONFilters, RawBlock, Header, Image

def eprint(*args, **kwargs):
    print(*args, file=sys.stderr, **kwargs)

def headers(key, value, fmt, meta):
    if key == 'Header' and value[0] == 1 and not re.match('Chapter|Disclaimer',value[2][0]['c'].encode('ascii','ignore')):
        return [Header(value[0]+1, value[1], value[2])]

def links_to_local(key, value, fmt, meta):
    if key == 'Link':
        if value[2][0][-3:] == '.md':
            with open(value[2][0], 'r') as f:
                first_line = f.readline()
                if first_line[0] == '#':
                    value[2][0] = '#' + first_line[2:].lower().replace(' ', '-').strip()
                else:
                    value[2][0] = '#' + value[2][0][:-3]

if __name__ == "__main__":
  toJSONFilters([headers,links_to_local])
