#!/usr/bin/env python3
"""Run the EVL suite using a local Papyrus/Epsilon installation, without a GUI.

Set EPSILON_PLUGINS or pass --plugins to choose the directory containing jars.
Only temporary models are created; repository application models are never loaded.
"""
import argparse
import os
from pathlib import Path
import re
import subprocess
import tempfile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--filter', help='Run only behavioral cases containing this text')
    parser.add_argument('--plugins', type=Path, default=Path(os.environ.get(
        'EPSILON_PLUGINS', str(Path.home() / 'Applications/papyrus-7.1/Papyrus/plugins'))))
    args = parser.parse_args()
    prefixes = ('org.eclipse.epsilon.', 'org.eclipse.emf.', 'org.eclipse.uml2.',
                'org.antlr.runtime_', 'org.eclipse.core.', 'org.eclipse.equinox.',
                'org.eclipse.osgi_', 'org.osgi.', 'com.google.guava', 'org.apache.commons.')
    jars = sorted(p for p in args.plugins.glob('*.jar')
                  if p.name.startswith(prefixes) and '.source_' not in p.name)
    if not any(p.name.startswith('org.eclipse.epsilon.evl.engine_') for p in jars):
        parser.error('Epsilon EVL jars not found; specify --plugins or EPSILON_PLUGINS')
    root = Path(__file__).resolve().parent.parent
    declared = re.findall(r'^\| (CON-\d+) \|',
                          (root / 'documentation/mappings.md').read_text(), re.MULTILINE)
    implemented = re.findall(r"message: '(CON-\d+)",
                             (root / 'validation/microservices.evl').read_text())
    if len(implemented) != len(set(implemented)) or set(declared) != set(implemented):
        parser.error('EVL constraint IDs must match the mappings exactly, with no duplicates')
    classpath = os.pathsep.join(str(p) for p in jars)
    with tempfile.TemporaryDirectory(prefix='microservices-evl-') as classes:
        subprocess.run(['javac', '-encoding', 'UTF-8', '-cp', classpath, '-d', classes,
                        str(root / 'validation/tests/MicroservicesValidationTest.java')], check=True)
        java_options = ['-Devl.filter=' + args.filter] if args.filter else []
        subprocess.run(['java', '-Xmx768m', *java_options, '-cp', classes + os.pathsep + classpath,
                        'MicroservicesValidationTest', str(root)], check=True)


if __name__ == '__main__':
    try:
        main()
    except subprocess.CalledProcessError as error:
        raise SystemExit(error.returncode) from None
