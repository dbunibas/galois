#!/usr/bin/env python3
"""
Make sure to run:
python src/cli/cli_main.py pz reg --path galois/core/src/test/resources/rag-fortune/documents --name galois-fortune-rag
(or pz reg ...)
"""

import argparse
import json
import os
import sys
import time

import pandas as pd
from tabulate import tabulate

import palimpzest as pz
from palimpzest.dataclasses import ExecutionStats
from palimpzest.elements.groupbysig import GroupBySig
from palimpzest.policy import MaxQualityAtFixedCost
from palimpzest.utils import token_tracker, udfs
from palimpzest.utils.env_helpers import load_env

load_env()


class FortuneCompany(pz.TextFile):
    """Represents a FortuneCompany, typically recorded in a structured format or report."""
    Company = pz.StringField(
        desc="Company", required=True,
    )

class FortuneCompanyRank(pz.TextFile):
    """Represents a FortuneCompanyRank, typically recorded in a structured format or report."""

    Rank = pz.NumericField(
        desc="Rank", required=True,
    )
    Company = pz.StringField(
        desc="Company", required=True,
    )

class FortuneCompanyCEOHeadquartersState(pz.TextFile):
    """Represents a FortuneCompanyCEOHeadquartersState, typically recorded in a structured format or report."""
    CompanyName = pz.StringField(
        desc="Company", required=True,
    )
    CEO = pz.StringField(
        desc="CEO", required=False,
    )
    HeadquartersState = pz.StringField(
        desc="HeadquartersState", required=False,
    )

class FortuneCompanyNumberOfEmpHeadquartersState(pz.TextFile):
    """Represents a FortuneCompanyNumberOfEmpHeadquartersState, typically recorded in a structured format or report."""
    CompanyName = pz.StringField(
        desc="Company", required=True,
    )
    Number_of_employees = pz.NumericField(
        desc="Number_of_employees", required=False,
    )
    HeadquartersState = pz.StringField(
        desc="HeadquartersState", required=False,
    )


class FortuneCompanyHeadquartersState(pz.TextFile):
    """Represents a FortuneCompanyHeadquartersState, typically recorded in a structured format or report."""
    Company = pz.StringField(
        desc="Company", required=True,
    )
    HeadquartersState = pz.StringField(
        desc="HeadquartersState", required=False,
    )

class FortuneHeadquartersCityIndustry(pz.TextFile):
    """Represents a FortuneHeadquartersCityIndustry, typically recorded in a structured format or report."""
    HeadquartersCityName = pz.StringField(
        desc="HeadquartersCity", required=False,
    )
    Industry = pz.StringField(
        desc="Industry", required=False,
    )


class FortuneHeadquartersCity(pz.TextFile):
    """Represents a v, typically recorded in a structured format or report."""
    HeadquartersCity = pz.StringField(
        desc="HeadquartersCity", required=False,
    )

class FortuneCompanySectorFounderIsCEOIsProfitable(pz.TextFile):
    """Represents a FortuneCompanySectorFounderIsCEOIsProfitable, typically recorded in a structured format or report."""
    CompanyName = pz.StringField(
        desc="Company", required=True,
    )
    Sector = pz.StringField(
        desc="Sector", required=False,
    )
    Is_Profitable = pz.BooleanField(
        desc="Is_Profitable", required=False,
    )

class FortuneCeoIsFemaleCeoPrivatePublic(pz.TextFile):
    """Represents a FortuneCeoIsFemaleCeoPrivatePublic, typically recorded in a structured format or report."""

    CEOName = pz.StringField(
        desc="CEO", required=False,
    )
    Is_FemaleCEO = pz.BooleanField(
        desc="Is_FemaleCEO", required=False,
    )
    private_or_public = pz.StringField(
        desc="private_or_public", required=False,
    )

class FortuneCeo(pz.TextFile):
    """Represents a FortuneCeo, typically recorded in a structured format or report."""

    CEO = pz.StringField(
        desc="CEO", required=False,
    )

class FortuneCompanyBestCompaniesToWorkIndustry(pz.TextFile):
    """Represents a FortuneCompanyBestCompaniesToWorkIndustry, typically recorded in a structured format or report."""
    CompanyName = pz.StringField(
        desc="Company", required=True,
    )
    Industry = pz.StringField(
        desc="Industry", required=False,
    )
    Best_Companies_to_Work_For = pz.BooleanField(
        desc="Best_Companies_to_Work_For", required=False,
    )

class FortuneCompanyCeoIsProfitableIsFemaleCeoHeadQuarterState(pz.TextFile):
    """Represents a FortuneCompanyCeoIsProfitableIsFemaleCeoHeadQuarterState, typically recorded in a structured format or report."""
    CompanyName = pz.StringField(
        desc="Company", required=True,
    )
    Is_Profitable = pz.BooleanField(
        desc="Is_Profitable", required=False,
    )
    Is_FemaleCEO = pz.BooleanField(
        desc="Is_FemaleCEO", required=False,
    )
    HeadquartersState = pz.StringField(
        desc="HeadquartersState", required=False,
    )

class FortuneCompanyHeadQuarterStateTickerCeoFounderIsCeoIsFemaleCEONumOfEmp(pz.TextFile):
    """Represents a FortuneCompanyHeadQuarterStateTickerCeoFounderIsCeoIsFemaleCEONumOfEmp, typically recorded in a structured format or report."""
    Company = pz.StringField(
        desc="Company", required=True,
    )
    Ticker = pz.StringField(
        desc="Ticker", required=False,
    )
    Founder_is_CEO = pz.BooleanField(
        desc="Founder_is_CEO", required=False,
    )
    Is_FemaleCEO = pz.BooleanField(
        desc="Is_FemaleCEO", required=False,
    )
    CEO = pz.StringField(
        desc="CEO", required=False,
    )
    Number_of_employees = pz.NumericField(
        desc="Number_of_employees", required=False,
    )
    HeadquartersState = pz.StringField(
        desc="HeadquartersState", required=False,
    )

class FortuneCompanyRankHeadQuarterStateTickerCeoFounderIsCeoIsFemaleCEONumOfEmp(pz.TextFile):
    """Represents a FortuneCompanyRankHeadQuarterStateTickerCeoFounderIsCeoIsFemaleCEONumOfEmp, typically recorded in a structured format or report."""
    CompanyName = pz.StringField(
        desc="Company", required=True,
    )
    Rank = pz.NumericField(
        desc="Rank", required=True,
    )
    Ticker = pz.StringField(
        desc="Ticker", required=False,
    )
    Founder_is_CEO = pz.BooleanField(
        desc="Founder_is_CEO", required=False,
    )
    Is_FemaleCEO = pz.BooleanField(
        desc="Is_FemaleCEO", required=False,
    )
    CEO = pz.StringField(
        desc="CEO", required=False,
    )
    Number_of_employees = pz.NumericField(
        desc="Number_of_employees", required=False,
    )
    HeadquartersState = pz.StringField(
        desc="HeadquartersState", required=False,
    )


class FortuneCompanyCeo(pz.TextFile):
    """Represents a FortuneCompanyCeo, typically recorded in a structured format or report."""
    Company = pz.StringField(
        desc="Company", required=True,
    )
    CEO = pz.StringField(
        desc="CEO", required=False,
    )


def print_table(records, policy, variant: int, cols=None, gradio=False, plan_str=None):
    records = [{key: record[key] for key in record.get_fields()} for record in records]
    records_df = pd.DataFrame(records)
    cols = [col for col in cols if col in records_df.columns]
    filtered_df = records_df[cols]
    print(tabulate(filtered_df, headers='keys', tablefmt='grid'))
    filtered_df.to_csv(f'exp-results/{policy}/{EXP_NAME}-Q{variant}.csv', sep=';', encoding='utf-8', index=False, header=True)

def get_exp(variant: int):

    if variant == 1:
        #.queryNum("Q1")
        #.querySql("select f.rank, f.company from fortune_2024 f order by f.rank asc limit 10")
        fortune_companies_md = pz.Dataset("galois-fortune-rag", schema=FortuneCompanyRank)
        fortune_companies_md = fortune_companies_md.filter("rank <= 10")
        return fortune_companies_md
    if variant == 2:
        #.queryNum("Q2")
        #.querySql("select company, ceo from fortune_2024 f where f.headquartersstate = 'Oklahoma'")
        fortune_companies_md = pz.Dataset("galois-fortune-rag", schema=FortuneCompanyCEOHeadquartersState)
        fortune_companies_md = fortune_companies_md.filter("headquartersstate = 'Oklahoma'")
        fortune_companies_md = fortune_companies_md.convert(FortuneCompanyCeo)
        return fortune_companies_md
    if variant == 3:
        #.queryNum("Q3")
        #.querySql("select company, headquartersstate from fortune_2024 where number_of_employees > 1000000")
        fortune_companies_md = pz.Dataset("galois-fortune-rag", schema=FortuneCompanyNumberOfEmpHeadquartersState)
        fortune_companies_md = fortune_companies_md.filter("number_of_employees > 1000000")
        fortune_companies_md = fortune_companies_md.convert(FortuneCompanyHeadquartersState)
        return fortune_companies_md
    if variant == 4:
        #.queryNum("Q4")
        #.querySql("select headquarterscity from fortune_2024 f where f.industry = 'Airlines'")
        fortune_companies_md = pz.Dataset("galois-fortune-rag", schema=FortuneHeadquartersCityIndustry)
        fortune_companies_md = fortune_companies_md.filter("industry = 'Airlines'")
        fortune_companies_md = fortune_companies_md.convert(FortuneHeadquartersCity)
        return fortune_companies_md
    if variant == 5:
        #.queryNum("Q5")
        #.querySql("select company from fortune_2024 f where f.sector = 'Technology' and founder_is_ceo = true and is_profitable = true")
        fortune_companies_md = pz.Dataset("galois-fortune-rag", schema=FortuneCompanySectorFounderIsCEOIsProfitable)
        fortune_companies_md = fortune_companies_md.filter("sector = 'Technology'")
        fortune_companies_md = fortune_companies_md.filter("founder_is_ceo = true")
        fortune_companies_md = fortune_companies_md.filter("is_profitable = true")
        fortune_companies_md = fortune_companies_md.convert(FortuneCompany)
        return fortune_companies_md
    if variant == 6:
        #.queryNum("Q6")
        #.querySql("select ceo from fortune_2024 f where  is_femaleceo = true and f.private_or_public = 'Private'")
        fortune_companies_md = pz.Dataset("galois-fortune-rag", schema=FortuneCeoIsFemaleCeoPrivatePublic)
        fortune_companies_md = fortune_companies_md.filter("is_femaleceo = true")
        fortune_companies_md = fortune_companies_md.filter("private_or_public = 'Private'")
        fortune_companies_md = fortune_companies_md.convert(FortuneCeo)
        return fortune_companies_md
    if variant == 7:
        #.queryNum("Q7")
        #.querySql("select company from fortune_2024 where best_companies_to_work_for = true and industry = 'Airlines'")
        fortune_companies_md = pz.Dataset("galois-fortune-rag", schema=FortuneCompanyBestCompaniesToWorkIndustry)
        fortune_companies_md = fortune_companies_md.filter("best_companies_to_work_for = true")
        fortune_companies_md = fortune_companies_md.filter("industry = 'Airlines'")
        fortune_companies_md = fortune_companies_md.convert(FortuneCompany)
        return fortune_companies_md
    if variant == 8:
        #.queryNum("Q8")
        #.querySql("select company, ceo from fortune_2024 f where f.is_profitable = true and f.is_femaleceo = true and f.headquartersstate = 'Texas'")
        fortune_companies_md = pz.Dataset("galois-fortune-rag", schema=FortuneCompanyCeoIsProfitableIsFemaleCeoHeadQuarterState)
        fortune_companies_md = fortune_companies_md.filter("is_femaleceo = true")
        fortune_companies_md = fortune_companies_md.filter("headquartersstate = 'Texas'")
        fortune_companies_md = fortune_companies_md.convert(FortuneCompanyCeo)
        return fortune_companies_md
    if variant == 9:
        #.queryNum("Q9")
        #.querySql("select company, headquartersstate, ticker, ceo, founder_is_ceo, is_femaleceo, number_of_employees from fortune_2024 where company = 'Nvidia'")
        fortune_companies_md = pz.Dataset("galois-fortune-rag", schema=FortuneCompanyHeadQuarterStateTickerCeoFounderIsCeoIsFemaleCEONumOfEmp)
        fortune_companies_md = fortune_companies_md.filter("company = 'Nvidia'")
        return fortune_companies_md
    if variant == 10:
        #.queryNum("Q10")
        #.querySql("select company, headquartersstate, ticker, ceo, founder_is_ceo, is_femaleceo, number_of_employees from fortune_2024 where headquarterscity = 'Santa Clara' and rank < 70")
        fortune_companies_md = pz.Dataset("galois-fortune-rag", schema=FortuneCompanyRankHeadQuarterStateTickerCeoFounderIsCeoIsFemaleCEONumOfEmp)
        fortune_companies_md = fortune_companies_md.filter("headquarterscity = 'Santa Clara'")
        fortune_companies_md = fortune_companies_md.filter("rank < 70")
        fortune_companies_md = fortune_companies_md.convert(FortuneCompanyHeadQuarterStateTickerCeoFounderIsCeoIsFemaleCEONumOfEmp)
        return fortune_companies_md
    
    raise ValueError("Unknown variant")


if __name__ == "__main__":

    EXP_NAME = 'Galois-Fortune-RAG'

    for variant in range(4, 5):
        print(variant)

        startTime = time.time()
        # #Sentinel -> sampling
        # engine = pz.SequentialSingleThreadSentinelExecution
        # engine = pz.PipelinedSingleThreadSentinelExecution
        # engine = pz.PipelinedParallelSentinelExecution
        # #NoSentinel -> no sampling
        engine = pz.SequentialSingleThreadNoSentinelExecution
        # engine = pz.PipelinedSingleThreadNoSentinelExecution
        # engine = pz.PipelinedParallelNoSentinelExecution

        # no_cache:
        # pz.DataDirectory().clear_cache(keep_registry=True)

        # policy = pz.MinCost()
        policy = MaxQualityAtFixedCost(max_cost=1.0) #Suggerito da Gerardo
        # policy = pz.MaxQuality()
        print(f'Policy: {policy}')

        result = get_exp(variant=variant)
        columns = [item for item in result.schema.field_names() if item not in ['contents', 'filename']]

        tables, stats = pz.Execute(
            result,
            policy=policy,
            nocache=True,
            allow_code_synth=False,
            allow_token_reduction=False,
            execution_engine=engine,
            verbose=True
        )

        stats: ExecutionStats = stats
        stats.total_used_tokens = token_tracker.getUsedInputToken() + token_tracker.getUsedOutputToken()

        print(f'##### {EXP_NAME}-Q{variant} ####')
        print(f'Stats - total_used_tokens: {token_tracker.getUsedInputToken() + token_tracker.getUsedOutputToken()}')
        print(f'Stats - total_execution_cost: {stats.total_execution_cost}')
        print(f'Stats - total_execution_time: {stats.total_execution_time}')
        print('Result')
        print_table(tables, policy=policy, variant=variant, cols=columns)

        endTime = time.time()
        print("Elapsed time:", endTime - startTime)
        with open(f'exp-results/{policy}/{EXP_NAME}-Q{variant}.json', "w") as f:
            json.dump(stats.to_json(), f)

        token_tracker.reset()
        print(f'Stats - total_used_tokens: {token_tracker.getUsedInputToken() + token_tracker.getUsedOutputToken()}')