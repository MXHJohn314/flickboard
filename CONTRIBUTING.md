# AI Policy

Contributions that use AI (ChatGPT, Gemini, Copilot, etc) will not be accepted.

# Layouts

Generally, the easiest way to contribute a new layout is by copying and modifying the
[english layout](https://codeberg.org/natkr/flickboard/src/branch/main/app/src/main/java/se/nullable/flickboard/model/layouts)
for your needs.

# Bugs and Feature Requests

Bugs and feature requests can be reported at https://codeberg.org/natkr/flickboard/issues.
Please try to search for whether an existing issue already exists for your topic.

# Pull Requests

Generally, pull requests for layouts that follow the general conventions are welcome.

Pull requests for features and bugfixes depend on the complexity of the issue, and should first
be discussed in their corresponding issue. While I'd like to support (almost) every feature, I have
to prioritize ensuring that the codebase stays maintainable (to me).

After you have opened a PR, please try to avoid force-pushes whenever possible:

- Prefer merging upstream into your branch to rebasing atop of upstream
- Revert your commits rather than resetting or rebasing them away
- Do not "clean up" or otherwise modify the history
- Do not squash your commits

Other guidelines:

- Commit frequently; tiny steps rather than larger steps; even if they are one logical step. Each time you compile, you should probably commit.
- Prefer code comments to explaining changes in commit messages
- Do not use "conventional commit messages"; just describe your changes
