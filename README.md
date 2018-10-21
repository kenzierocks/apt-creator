apt-creator
===============
Annotation processor for making factory/creator implementations, like
[Auto Factory]. Supports less features, but was written since Auto Factory
had not updated to JDK 9+ in many months.

## Usage
`apt-creator` is very similar to Auto Factory. Simply annotate your class with
`@GenerateCreator`, and any parameters that should be injected (via JSR 330)
with `@Provided`.

Multiple constructors are supported as long as they don't create duplicate
signatures after any `@Provided` arguments are dropped.

The generated class will be named the same way that Auto Factory names
factories. A top-level class `Foo` will generate `FooCreator`. A class `Bar`
nested inside `Biz` will generate `Biz_BarCreator`. You may override the name
like with Auto Factory by setting `className`, e.g.
`@GenerateCreator(className = "FezFactory")` will generate a class named
`FezFactory`. This allows for full control over the name if needed.

[Auto Factory]: https://github.com/google/auto/tree/master/factory
