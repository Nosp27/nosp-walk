import json

import flask
from datetime import datetime
import psycopg2
from flask import make_response
from werkzeug import exceptions
import waitress


app = flask.Flask(__name__)


def query(sql, *args):
    with psycopg2.connect("dbname=nosp_walk user=postgres") as conn:
        with conn.cursor() as cursor:
            cursor.execute(sql, *args)
            return list(cursor)


@app.route("/")
def main_page():
    return {"status": "fine"}


@app.route("/sign_in")
def sign_in(request: flask.Request):
    if request.method != "POST":
        raise exceptions.MethodNotAllowed()

    json_data = json.loads(request.data)
    key_hash = json_data["key"]
    user = json_data["user"]
    granted = (
        len(
            query(
                f"""
    select * from users
    where key_hash = {key_hash}
    """
            )
        )
        == 1
    )
    if not granted:
        raise exceptions.Forbidden()

    resp = make_response({"access": "granted"})
    resp.set_cookie("user", user)
    return resp


@app.route("/history")
def get_history(request: flask.Request):
    if request.method != "GET":
        raise exceptions.MethodNotAllowed()

    req_data = request.args
    from_date, to_date = (req_data["from"], req_data["to"])
    walks = query(
        f"""
    select * from walks
    where walked_at between $1 and $2 
    """,
        datetime.fromtimestamp(from_date),
        datetime.fromtimestamp(to_date),
    )

    walks = [
        {"date": w["date"], "time": w["time"], "walker": w["walker"]} for w in walks
    ]
    return walks


@app.route("/walk")
def walk(request: flask.Request):
    if request.method != "POST":
        raise exceptions.MethodNotAllowed()

    json_data = json.loads(request.data)

    walk_timestamp = json_data["timestamp"]
    walker = json_data["walker"]
    assert walker in ("P", "M")

    if request.cookies.get("user") != walker:
        raise exceptions.Forbidden()

    query(
        f"""
    insert into walks (walker, walked_at) values ($1, $2)
    """,
        datetime.fromtimestamp(walk_timestamp),
        walker,
    )

    return {"status": "fine"}
