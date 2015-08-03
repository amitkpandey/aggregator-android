# -*- coding: utf-8 -*-

site.url = "http://tughi.github.io/aggregator-android/"
site.author = "Tughi"

site.file_ignore_patterns = [
    ".*/_.*",
    ".*/\..*",
]


def post_build():
    import os
    import shutil

    os.chdir("_site")
    for root, dirs, files in os.walk("."):
        if root.startswith("./"):
            root = root[2:]
        try:
            os.mkdir(os.path.join("../_gh-pages", root))
        except OSError:
            pass

        for file in files:
            shutil.copy2(os.path.join(root, file), os.path.join("../_gh-pages", root))
