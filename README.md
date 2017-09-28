[![Build Status](https://travis-ci.org/PavlikPolivka/GitLabProjects.svg?branch=master)](https://travis-ci.org/PavlikPolivka/GitLabProjects)

## GitLab Projects
Simple plugin that is adding support for GitLab specific actions to JetBrain IDEs

### Features:
* **GitLab Checkout dialog** - list all users projects on GitLab and provides quick way to clone them locally
* **GitLab Share dialog** - allows quick import of new projects to GitLab, user can specify namespace and project visibility
* **GitLab Merge Request dialog**  - user can quickly create new merge requests from current branch
* **GitLab Merge Request List dialog**  - user can list and accept all open code reviews

### Development
This project is using the intellij gradle build plugin. 
https://github.com/JetBrains/gradle-intellij-plugin

To build do

`gradle build`

To run it in IDE and debug

`gradle runIdea`


### Contributing
Everybody is welcome to contribute to this project.

Submit your pull requests to **master** branch.

Master is always exact code that is used in latest release.