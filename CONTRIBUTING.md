# Contributing to BreweryX

## Code Contributions

### Code Styling

Code should follow the overall project's code style.
We follow [Oracle's Standard Programming Practices](https://www.oracle.com/java/technologies/javase/codeconventions-programmingpractices.html)
with the exception of `10.5.1 Parentheses`. As long as your operator expressions are
*clear enough*, you should be fine.

### License Headers

Not required for submitting PRs to BreweryX. IntelliJ IDEs should recognize
the copyright included in the .idea folder automatically.

### Should Your Contribution Be an Addon?

Before contributing features to BreweryX, ask yourself "Should this be an addon instead?".
BreweryX has a mostly-extensive addon API which allows you to create plugin-like addons for
BreweryX. Let's take a look at some examples that should be an addon vs what should be a contribution to BreweryX:

- A contribution that adds support for another plugin's custom items <-- ✅ **Great as a Contribution**
- A contribution which adds GUIs to interface with cauldrons <-- ❌ **Changing the core gameplay of Brewery should not be a contribution, write this as an addon instead**
- A contribution which patches bugs, exploits, or improves performance of BreweryX <-- ✅ **Great as a Contribution**
- A contribution which adds growable plants which can be used in recipes  <-- ❌ **Bloating BreweryX with somewhat-niche features should not be a contribution, write this as an addon instead**

Want to get started on building a BreweryX addon? See our wiki page on [Creating an Addon](https://brewery.lumamc.net/api/api/#creating_an_addon).

### Don't Use Local Libraries

Local libraries should not be used. Libraries should be declared in `build.gradle.kts` with their
repository and **Gradle KTS (Short)** annotation. If you *must* use a local jar library for a contribution
**explain clearly** why you are using a local library and if there are any other options rather than using
a local library.

## Translation & Wiki Contributions

### Translation Contributions

Translations for BreweryX are always welcome! 

Translation contributions should include a `config-langs` and `languages` addition.
One being a translation for the all configuration files of BreweryX and the other being a translation for
messages sent by the plugin (lang).

Translation files should be named in **short language code**, e.g. `en.yml` for English and `zh.yml` for Chinese.

### How Can I Contribute Translations?

Some people contributing translations may not be familiar with the forking and PR'ing aspect of
Git. **See below** for instructions on how to fork, clone, and submit a PR.

### Wiki Contributions

Wiki contributions should have purpose and serve as informative pages to those learning how
to navigate BreweryX. When submitting a Wiki PR, make sure the information included is accurate, up-to-date,
and concise.


## Forking, Cloning, & Submitting Pull Requests

Start by [forking](https://github.com/BreweryTeam/BreweryX/fork) the BreweryX repository to your GitHub profile.
If contributing to a repository other than `master` (usually for code contributions), uncheck **☑️ Copy the `master` branch only**.

Once created, navigate to the brightly colored **</> Code** button just above the file list on the repository.
If you know what you're doing, just clone it using whatever method you'd like. If you're new to this, you can
fork the repository using GitHub Desktop and edit the files in the Code Editor of your choice.

If you're making Translation or Wiki contributions, [VS Code](https://code.visualstudio.com/) should be fine. If you're making programming
contributions, [IntelliJ IDEA Community Edition](https://www.jetbrains.com/idea/download/) is recommended if you don't already have an IDE/Code Editor of preference.

### Contributions through Git

Install [Git](https://git-scm.com/downloads) if you don't have it already.

1. Clone the repository.

`git remote add origin https://github.com/BreweryTeam/BreweryX.git`

`git pull origin master` (or another branch)

2. *Make your changes.*


3. Push back to your forked Repository of BreweryX

`git add --all`

`git commit -m "I added change XYZ~!"`

`git push origin master` (or another branch)


### Contributions Through Codespaces

On the **</> Code** button, click the `Codespaces` tab.

Learn how to use Codespaces: https://docs.github.com/en/codespaces

### Contributions Manually

I don't recommend editing files without the proper tools like [Git](https://git-scm.com/downloads) or [Codespaces](https://docs.github.com/en/codespaces) 
at your disposal. If you choose to contribute manually, feel free to, but there won't be a tutorial for
this. 


### Opening a Pull Request

GitHub should for the most part, prompts you to open a Pull Request as soon as you push changes
to your forked repository. Just navigate to the home page for your repository and follow GitHub's
prompts from there.


# That should be it. Thanks for contributing to BreweryX!