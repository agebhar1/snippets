import logging
import time

import uvicorn
from fastapi import FastAPI
from opentelemetry import trace

app = FastAPI()
tracer = trace.get_tracer(__name__)
logger = logging.getLogger("uvicorn")


@app.get("/")
async def hello() -> dict:
    current_span = trace.get_current_span()
    current_span.set_attribute("attribute", "attribute-value")
    current_span.add_event("event", {"a": "value a", "b": "value b"})

    trace_id = current_span.get_span_context().trace_id
    logger.info("before parent span TraceId=%s SpanId=%s", hex(trace_id)[2:], hex(current_span.get_span_context().span_id)[2:])

    with tracer.start_as_current_span("parent") as parent_span:
        logger.info("within parent span TraceId=%s SpanId=%s", hex(trace_id)[2:], hex(parent_span.get_span_context().span_id)[2:])
        time.sleep(0.1)
        with tracer.start_as_current_span("child") as child_span:
            logger.info("within child span TraceId=%s SpanId=%s", hex(trace_id)[2:], hex(child_span.get_span_context().span_id)[2:])
            time.sleep(0.2)
        time.sleep(0.1)

    return {"message": "Hello World"}


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)  # , log_config=None)
