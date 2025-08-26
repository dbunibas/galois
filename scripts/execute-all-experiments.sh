echo "Before continuing!"
echo
echo "This script will execute *all* the experiments of the “Logical and Physical Optimizations for SQL Query Execution over Large Language Models” using TogetherAI as the LLM provider."
echo
echo "Both providers (OpenAI and TogetherAI) will be used sequentially to execute the experiments."
echo
read -p "Start the execution of *all* the experiments? (y/N): " confirm && [[ $confirm == [yY] || $confirm == [yY][eE][sS] ]] || exit 1

cd ../executors
names=`ls ./*.sh`
for executor in $names
do
  sh $executor
done
:
