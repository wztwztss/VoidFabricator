# Compile-time dependency jars

Third-party jars are intentionally **not redistributed** in this repository.
Place the following exact filenames in this directory before building:

```text
libs/
  appliedenergistics2-rv3-beta-1083-GTNH.jar
  gregtech-5.09.54.205.jar
  modularui2-2.3.92-1.7.10.jar
  modularui-1.3.4.jar
  ae2-qol-4.5.0-fix30.jar
  NotEnoughItems-2.8.171-GTNH.jar
  CodeChickenCore-1.4.22.jar
```

These are the compile-time references used by `dependencies.gradle`.
They can be exported from an existing GTNH 1.7.10 development workspace.
Do not substitute different versions without checking the source APIs.

The published `VoidFabricator-0.3.1-alpha.jar` Release asset is the ready-to-use
mod jar; using the Release does not require this directory.
