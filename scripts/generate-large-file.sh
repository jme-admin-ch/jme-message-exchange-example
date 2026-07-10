#!/bin/bash

# File to write to
output_file="large-file.xml"

# Ensure the file is empty before writing
> "$output_file"

# Write XML header
echo "<root>" >> "$output_file"

for (( i=0; i<10000000; i++ )); do
  echo "<somexml><xmltag></xmltag></somexml>" >> "$output_file"
done

# Write XML footer
echo "</root>" >> "$output_file"

echo "XML generation completed. Output written to: $output_file"

