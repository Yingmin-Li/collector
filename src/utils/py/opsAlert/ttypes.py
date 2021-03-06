#
# Autogenerated by Thrift
#
# DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
#

from thrift.Thrift import *

from thrift.transport import TTransport
from thrift.protocol import TBinaryProtocol
try:
  from thrift.protocol import fastbinary
except:
  fastbinary = None


class OpsAlert:
  """
  Attributes:
   - source
   - from
   - to
   - alert
   - event_date
  """

  thrift_spec = (
    None, # 0
    (1, TType.STRING, 'source', None, None, ), # 1
    (2, TType.STRING, 'from', None, None, ), # 2
    (3, TType.STRING, 'to', None, None, ), # 3
    (4, TType.STRING, 'alert', None, None, ), # 4
    (5, TType.I64, 'event_date', None, None, ), # 5
  )

  def __init__(self, source=None, fromMsg=None, to=None, alert=None, event_date=None,):
    self.source = source
    self.fromMsg = fromMsg
    self.to = to
    self.alert = alert
    self.event_date = event_date

  def read(self, iprot):
    if iprot.__class__ == TBinaryProtocol.TBinaryProtocolAccelerated and isinstance(iprot.trans, TTransport.CReadableTransport) and self.thrift_spec is not None and fastbinary is not None:
      fastbinary.decode_binary(self, iprot.trans, (self.__class__, self.thrift_spec))
      return
    iprot.readStructBegin()
    while True:
      (fname, ftype, fid) = iprot.readFieldBegin()
      if ftype == TType.STOP:
        break
      if fid == 1:
        if ftype == TType.STRING:
          self.source = iprot.readString();
        else:
          iprot.skip(ftype)
      elif fid == 2:
        if ftype == TType.STRING:
          self.fromMsg = iprot.readString();
        else:
          iprot.skip(ftype)
      elif fid == 3:
        if ftype == TType.STRING:
          self.to = iprot.readString();
        else:
          iprot.skip(ftype)
      elif fid == 4:
        if ftype == TType.STRING:
          self.alert = iprot.readString();
        else:
          iprot.skip(ftype)
      elif fid == 5:
        if ftype == TType.I64:
          self.event_date = iprot.readI64();
        else:
          iprot.skip(ftype)
      else:
        iprot.skip(ftype)
      iprot.readFieldEnd()
    iprot.readStructEnd()

  def write(self, oprot):
    if oprot.__class__ == TBinaryProtocol.TBinaryProtocolAccelerated and self.thrift_spec is not None and fastbinary is not None:
      oprot.trans.write(fastbinary.encode_binary(self, (self.__class__, self.thrift_spec)))
      return
    oprot.writeStructBegin('OpsAlert')
    if self.source != None:
      oprot.writeFieldBegin('source', TType.STRING, 1)
      oprot.writeString(self.source)
      oprot.writeFieldEnd()
    if self.fromMsg != None:
      oprot.writeFieldBegin('from', TType.STRING, 2)
      oprot.writeString(self.fromMsg)
      oprot.writeFieldEnd()
    if self.to != None:
      oprot.writeFieldBegin('to', TType.STRING, 3)
      oprot.writeString(self.to)
      oprot.writeFieldEnd()
    if self.alert != None:
      oprot.writeFieldBegin('alert', TType.STRING, 4)
      oprot.writeString(self.alert)
      oprot.writeFieldEnd()
    if self.event_date != None:
      oprot.writeFieldBegin('event_date', TType.I64, 5)
      oprot.writeI64(self.event_date)
      oprot.writeFieldEnd()
    oprot.writeFieldStop()
    oprot.writeStructEnd()

  def __repr__(self):
    L = ['%s=%r' % (key, value)
      for key, value in self.__dict__.iteritems()]
    return '%s(%s)' % (self.__class__.__name__, ', '.join(L))

  def __eq__(self, other):
    return isinstance(other, self.__class__) and self.__dict__ == other.__dict__

  def __ne__(self, other):
    return not (self == other)

