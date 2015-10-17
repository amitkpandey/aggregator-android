import codecs
import jinja2
import os
import markdown
import shutil

SOURCES = {}
JINJA2 = jinja2.Environment(loader=jinja2.FileSystemLoader('templates'))
TEMPLATES = {
    'howto': JINJA2.get_template('howto.jinja2')
}


def process_content():
    os.chdir('content')
    for root, dirs, files in os.walk('./'):
        if root.startswith('./'):
            root = root[2:]

        try:
            os.mkdir(os.path.join('../gh-pages', root))
        except OSError:
            pass

        for file_name in files:
            file_path = os.path.join(root, file_name)

            if file_name.endswith('.source'):
                source = load_source(file_path)

                source_type = source['type']
                if source_type != 'news':
                    generate_html(source)
                pass
            else:
                print 'copying', file_path
                shutil.copy(file_path, os.path.join('../gh-pages', file_path))


def load_source(file_path):
    if file_path not in SOURCES:
        print 'loading', file_path

        with codecs.open(file_path, mode='r', encoding='utf-8') as reader:
            source = SOURCES[file_path] = {
                'source': file_path
            }

            for metadata in reader:
                metadata = metadata.strip()
                if not metadata:
                    break
                name, value = metadata.split(':', 1)
                name = name.strip()
                value = value.strip()

                source[name] = value

            source_content = reader.read()
            if source_content:
                source['content'] = jinja2.Markup(markdown.markdown(source_content))

    return SOURCES[file_path]


def generate_html(source):
    source_type = source['type']
    source_source = source['source']

    print 'generating', source_type, 'from', source_source

    template = TEMPLATES[source_type]

    with codecs.open(os.path.join('../gh-pages', source_source[:-6] + 'html'), mode='w', encoding='utf-8') as writer:
        writer.write(template.render(**source))


if __name__ == '__main__':
    process_content()
