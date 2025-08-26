echo "TODO: warning..."
read -p "Continue? (y/N): " confirm && [[ $confirm == [yY] || $confirm == [yY][eE][sS] ]] || exit 1

cd ../executors
names=`ls ./*-togetherai.sh`
for executor in $names
do
  sh $executor
done
:
