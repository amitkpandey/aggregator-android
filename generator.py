import codecs
import dateutil.parser
import jinja2
import os
import markdown
import shutil

SOURCES = {}
JINJA2 = jinja2.Environment(loader=jinja2.FileSystemLoader('templates'))
TEMPLATES = {
    'howto': JINJA2.get_template('howto.jinja2'),
    'news': JINJA2.get_template('news.jinja2'),
}


def process_content():
    news = []

    os.chdir('content')
    for root, dirs, files in os.walk('./'):
        if root.startswith('./'):
            root = root[2:]

        try:
            os.mkdir(os.path.join('../gh-pages', root))
        except OSError:
            pass

        for file_name in sorted(files):
            file_path = os.path.join(root, file_name)

            if file_name.endswith('.source'):
                source = load_source(file_path)

                source_type = source['type']
                if source_type == 'news':
                    news.append(source)
                else:
                    generate_html(source)

            else:
                print 'copying', file_path
                shutil.copy(file_path, os.path.join('../gh-pages', file_path))

    generate_rss(news)


def load_source(file_path):
    if file_path not in SOURCES:
        print 'loading', file_path

        with codecs.open(file_path, mode='r', encoding='utf-8') as reader:
            source = SOURCES[file_path] = {
                'file': file_path
            }

            for metadata in reader:
                metadata = metadata.strip()
                if not metadata:
                    break
                name, value = metadata.split(':', 1)
                name = name.strip()
                value = value.strip()

                if name == 'date':
                    source[name] = dateutil.parser.parse(value)
                else:
                    source[name] = value

            source_content = reader.read()
            if source_content:
                source['content'] = jinja2.Markup(markdown.markdown(source_content))

    return SOURCES[file_path]


def generate_html(source):
    source_type = source['type']
    source_file = source['file']

    print 'generating', source_type, 'from', source_file

    template = TEMPLATES[source_type]

    with codecs.open(os.path.join('../gh-pages', source_file[:-6] + 'html'), mode='w', encoding='utf-8') as writer:
        writer.write(template.render(**source))


def generate_rss(news):
    if not news:
        return

    print 'generating rss news'

    template = TEMPLATES['news']

    for item in news:
        if 'page' in item:
            page_source = SOURCES[item['page']].copy()
            page_source.update(item)
            item.update(page_source)

    with codecs.open(os.path.join('../gh-pages', 'news.rss'), mode='w', encoding='utf-8') as writer:
        writer.write(template.render({'news': news}))


if __name__ == '__main__':
    process_content()
