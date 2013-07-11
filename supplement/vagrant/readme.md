### Sandbox repackaging

1. cd mysql-X.X.XX-sandbox
2. vagrant up
3. vagrant package --output ../mysql-X.X.XX-sandbox.box
4. vagrant box add mysql-X.X.XX-sandbox ../mysql-X.X.XX-sandbox.box --force

