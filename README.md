# llmodify

Create a context aware prompt with structured output for multi file code modification.

## Features

- traverse source code for context capturing
- use xml data format for the most llm friendly interaction
- provide source code as a prompt context
- add line number for each line in source code for the most llm friendly input format
- instruct to generate xml 
- support source code compatible yet xml escapeless format using xml's CDATA feature
- consume assistant's result in xml format for one shot project modification

## Commands

| command | args | description |
| --- | --- | --- |
| context | -o, --output, -r, --repo | outputs xml with <context> tag |
| prompt | -o, --output, -r, --repo, -i, --instruction | build whole <prompt> with system, user, context, instruction, output format |
| modify | -i, --input, -r, --repo | takes xml from llm and do all file IO operations |
