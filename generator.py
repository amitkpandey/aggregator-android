import codecs
import jinja2
import os
import markdown
import shutil


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

            if file_name.endswith('.md'):
                generate_md(file_path)
            else:
                print 'copying', file_path
                shutil.copy(file_path, os.path.join('../gh-pages', file_path))


jinja2_env = None


def generate_md(file_path):
    print 'processing', file_path

    global jinja2_env
    if not jinja2_env:
        jinja2_env = jinja2.Environment(loader=jinja2.FileSystemLoader('../templates'))

    with codecs.open(file_path, mode='r', encoding='utf-8') as reader:
        template = None
        context = {}
        for metadata in reader:
            metadata = metadata.strip()
            if metadata == '*---':
                continue
            if metadata == '---*':
                break
            name, value = metadata.split(':', 1)
            name = name.strip()
            value = value.strip()

            if name == 'template':
                template = jinja2_env.get_template(value)
            else:
                context[name] = value

        context['content'] = jinja2.Markup(markdown.markdown(reader.read()))

        with codecs.open(os.path.join('../gh-pages', file_path[:-3] + '.html'), mode='w', encoding='utf-8') as writer:
            writer.write(template.render(**context))


if __name__ == '__main__':
    process_content()
